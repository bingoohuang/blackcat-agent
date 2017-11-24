package com.github.bingoohuang.blackcat.agent.collectors;

import com.alibaba.fastjson.util.IOUtils;
import com.github.bingoohuang.blackcat.sdk.netty.BlackcatReqSender;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.val;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@AllArgsConstructor
public class BlactcatLogExceptionCollector {
    private final BlackcatReqSender client;
    private final String logFiles;
    private final long rotateSeconds;

    @SneakyThrows
    public void start() {
        val loggers = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(logFiles);
        val processBeans = Lists.<ProcessBean>newArrayListWithCapacity(loggers.size());
        for (val logFile : loggers) {
            String[] loggerAndFile = logFile.split(":");
            val commands = new String[]{"/bin/bash", "-c", "tail -F " + loggerAndFile[1], "&"};

            processBeans.add(new ProcessBean(client, commands, loggerAndFile[0]));
        }

        if (processBeans.isEmpty()) return;

        val executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(() -> {
            for (val processBean : processBeans) {
                processBean.rotateCheck();
            }
        }, rotateSeconds, rotateSeconds, TimeUnit.SECONDS);
    }


    public static class LineReader {
        private final BufferedReader reader;
        private StringBuilder lineStringBuilder = new StringBuilder();

        public LineReader(BufferedReader reader) {
            this.reader = reader;
        }

        private String readLine() throws IOException {
            while (reader.ready()) {
                int chr = reader.read();
                if (chr == -1) break;

                lineStringBuilder.append((char) chr);
                if (chr == '\n') {
                    val line = lineStringBuilder.toString();
                    lineStringBuilder.setLength(0);
                    return line;
                }
            }

            return null;
        }
    }

    interface Consts {
        Pattern NORMAL_LINE_START = Pattern.compile("^([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}) .*", Pattern.DOTALL); // 2017-11-20 03:58:48.167
        Pattern EXCEPTION_PATTERN = Pattern.compile("\\b\\S+Exception:"); // java.io.InvalidClassException:
        // tenantCode[1704421450] tenantId[94c63603-e487-4cb3-bc98-6129ec616722]
        Pattern TCODE_PATTERN = Pattern.compile("tenantCode\\[(\\d+)\\]");
        Pattern TID_PATTERN = Pattern.compile("tenantId\\[(.*?)\\]");

        Joiner JOINER = Joiner.on("");
    }

    @Data @AllArgsConstructor
    private static class ProcessBean {
        private final BlackcatReqSender client;
        private final String[] commands;
        private final String logger;

        private Process process;
        private BufferedReader bufferedReader;
        private LineReader lineReader;

        private EvictingQueue<Object> evictingQueue = EvictingQueue.create(10);
        private List<String> exceptionStack = new ArrayList<>();

        public ProcessBean(BlackcatReqSender client, String[] commands, String logger) {
            this.client = client;
            this.commands = commands;
            this.logger = logger;

            createLineReader(commands);
        }

        public void reset() {
            process.destroy();
            IOUtils.close(bufferedReader);

            evictingQueue.clear();
            exceptionStack.clear();
            createLineReader(commands);
        }

        @SneakyThrows
        private void createLineReader(String[] commands) {
            process = Runtime.getRuntime().exec(commands);
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            lineReader = new LineReader(bufferedReader);
        }

        public void rotateCheck() {
            String lastLine = null;
            try {
                while (true) {
                    val line = lineReader.readLine();
                    if (line == null) break;

                    if (Consts.NORMAL_LINE_START.matcher(line).matches()) {
                        detectException(lastLine);
                        evictingQueue.add(line);
                        lastLine = line;
                    } else {
                        exceptionStack.add(line);
                    }
                }
            } catch (IOException ex) {
                reset();
            }

            detectException(lastLine);
        }

        private void detectException(String lastLine) {
            if (exceptionStack.isEmpty()) return;

            evictingQueue.add(Consts.JOINER.join(exceptionStack));
            findException(lastLine);
            exceptionStack.clear();
        }

        private void findException(String lastLine) {
            val exceptionNames = new StringBuilder();
            for (val line : exceptionStack) {
                val matcher = Consts.EXCEPTION_PATTERN.matcher(line);
                if (matcher.find()) {
                    exceptionNames.append(line);
                }
            }

            if (exceptionNames.length() == 0) return;

            // exception found
            val blackcatReq = createBlackcatException(lastLine, exceptionNames);

            client.send(blackcatReq);
        }

        private BlackcatMsg.BlackcatReq createBlackcatException(String lastLine, StringBuilder exceptionNames) {
            val tcode = findTcode(lastLine);
            val tid = findTid(lastLine);
            val timestamp = findTimestamp(lastLine);

            val blackcatExceptionBuilder = BlackcatMsg.BlackcatException.newBuilder()
                    .setLogger(logger)
                    .setTcode(tcode)
                    .setTid(tid)
                    .setExceptionNames(exceptionNames.toString())
                    .setTimestamp(timestamp)
                    .setContextLogs(Consts.JOINER.join(evictingQueue));

            return BlackcatMsg.BlackcatReq.newBuilder()
                    .setBlackcatReqHead(Blackcats.buildHead(BlackcatMsg.BlackcatReqHead.ReqType.BlackcatException))
                    .setBlackcatException(blackcatExceptionBuilder)
                    .build();
        }

        private String findTimestamp(String lastLine) {
            if (lastLine == null) return "unknown";

            val matcher = Consts.EXCEPTION_PATTERN.matcher(lastLine);
            return matcher.find() ? matcher.group(1) : "unknown";
        }

        private String findTid(String lastLine) {
            if (lastLine == null) return "unknown";

            val matcher = Consts.TID_PATTERN.matcher(lastLine);
            return matcher.find() ? matcher.group(1) : "unknown";
        }

        private String findTcode(String lastLine) {
            if (lastLine == null) return "unknown";

            val matcher = Consts.TCODE_PATTERN.matcher(lastLine);
            return matcher.find() ? matcher.group(1) : "unknown";
        }
    }
}
