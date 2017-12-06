package com.github.bingoohuang.blackcat.agent.collectors;

import com.alibaba.fastjson.util.IOUtils;
import com.github.bingoohuang.blackcat.sdk.netty.BlackcatReqSender;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.github.bingoohuang.blackcat.sdk.utils.ProcessExecutor;
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

import static com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType.BlackcatLogException;

@AllArgsConstructor
public class BlactcatLogExceptionCollector {
    private final BlackcatReqSender sender;
    private final String logFiles;
    private final long rotateSeconds;

    @SneakyThrows
    public void start() {
        val processBeans = parseLogConfigs();
        if (processBeans.isEmpty()) return;

        val executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(() -> {
            for (val processBean : processBeans) {
                processBean.rotateCheck();
            }
        }, rotateSeconds, rotateSeconds, TimeUnit.SECONDS);
    }

    private List<ProcessBean> parseLogConfigs() {
        val loggers = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(logFiles);
        val processBeans = Lists.<ProcessBean>newArrayListWithCapacity(loggers.size());
        for (val logFile : loggers) {
            val loggerAndFile = logFile.split(":");
            val commands = new String[]{"/bin/bash", "-c", "tail -F " + loggerAndFile[1], "&"};

            processBeans.add(new ProcessBean(sender, commands, loggerAndFile[0]));
        }
        return processBeans;
    }


    public static class LineReader {
        private final BufferedReader reader;
        private StringBuilder lineStringBuilder = new StringBuilder();

        public LineReader(BufferedReader reader) {
            this.reader = reader;
        }

        private String readLine() throws IOException {
            while (reader.ready()) {
                val chr = reader.read();
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
        Pattern NORMAL_LINE_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) .*", Pattern.DOTALL); // 2017-11-20 03:58:48.167
        Pattern EXCEPTION_PATTERN = Pattern.compile("\\b\\S+Exception:"); // java.io.InvalidClassException:
        // tenantCode[1704421450] tenantId[94c63603-e487-4cb3-bc98-6129ec616722]
        Pattern TCODE_PATTERN = Pattern.compile("tenantCode\\[(\\d+)\\]");
        Pattern TID_PATTERN = Pattern.compile("tenantId\\[(.*?)\\]");

        Joiner JOINER = Joiner.on("");
    }

    @Data @AllArgsConstructor
    private static class ProcessBean {
        private final BlackcatReqSender sender;
        private final String[] commands;
        private final String logger;

        private Process process;
        private BufferedReader bufferedReader;
        private LineReader lineReader;

        private EvictingQueue<Object> evictingQueue = EvictingQueue.create(10);
        private List<String> exceptionStack = new ArrayList<>();

        public ProcessBean(BlackcatReqSender sender, String[] commands, String logger) {
            this.sender = sender;
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
            if (!ProcessExecutor.isAlive(process)) {
                reset();
            }

            String lastNormalLine = null;
            try {
                while (true) {
                    val line = lineReader.readLine();
                    if (line == null) break;

                    if (Consts.NORMAL_LINE_PATTERN.matcher(line).matches()) {
                        detectException(lastNormalLine);
                        evictingQueue.add(line);
                        lastNormalLine = line;
                    } else {
                        exceptionStack.add(line);
                    }
                }
            } catch (Exception ex) {
                reset();
            }

            detectException(lastNormalLine);
        }

        private void detectException(String lastNormalLine) {
            if (exceptionStack.isEmpty()) return;

            evictingQueue.add(Consts.JOINER.join(exceptionStack));
            findException(lastNormalLine);
            exceptionStack.clear();
        }

        private void findException(String lastNormalLine) {
            val exceptionNames = new StringBuilder();
            for (val line : exceptionStack) {
                val matcher = Consts.EXCEPTION_PATTERN.matcher(line);
                if (matcher.find()) {
                    exceptionNames.append(line);
                }
            }

            if (exceptionNames.length() == 0) return;

            // exception found
            val blackcatReq = createBlackcatLogException(lastNormalLine, exceptionNames);

            sender.send(blackcatReq);
        }

        private BlackcatMsg.BlackcatReq createBlackcatLogException(String lastLine, StringBuilder exceptionNames) {
            val tcode = findTcode(lastLine);
            val tid = findTid(lastLine);
            val timestamp = findTimestamp(lastLine);

            val blackcatLogExceptionBuilder = BlackcatMsg.BlackcatLogException.newBuilder()
                    .setLogger(logger)
                    .setTcode(tcode)
                    .setTid(tid)
                    .setExceptionNames(exceptionNames.toString())
                    .setTimestamp(timestamp)
                    .setContextLogs(Consts.JOINER.join(evictingQueue));

            return BlackcatMsg.BlackcatReq.newBuilder()
                    .setBlackcatReqHead(Blackcats.buildHead(BlackcatLogException))
                    .setBlackcatLogException(blackcatLogExceptionBuilder)
                    .build();
        }

        private String findTimestamp(String line) {
            if (line == null) return "unknown";

            val matcher = Consts.NORMAL_LINE_PATTERN.matcher(line);
            return matcher.find() ? matcher.group(1) : "unknown";
        }

        private String findTid(String line) {
            if (line == null) return "unknown";

            val matcher = Consts.TID_PATTERN.matcher(line);
            return matcher.find() ? matcher.group(1) : "unknown";
        }

        private String findTcode(String line) {
            if (line == null) return "unknown";

            val matcher = Consts.TCODE_PATTERN.matcher(line);
            return matcher.find() ? matcher.group(1) : "unknown";
        }
    }
}
