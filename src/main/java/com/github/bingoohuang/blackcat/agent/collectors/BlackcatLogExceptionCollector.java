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
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.n3r.diamond.client.Miner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@AllArgsConstructor @Slf4j
public class BlackcatLogExceptionCollector {
    private final BlackcatReqSender sender;
    private final String logFiles;
    private final long rotateSeconds;
    private final long ignoreMillisBefore;

    public BlackcatLogExceptionCollector(BlackcatReqSender sender, String logFiles, long rotateSeconds) {
        this(sender, logFiles, rotateSeconds, 3 * 60 * 60 * 1000L);
    }

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

            processBeans.add(new ProcessBean(sender, commands, loggerAndFile[0], ignoreMillisBefore));
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
        Pattern EXCEPTION_PATTERN = Pattern.compile("\\b\\S+Exception\\b"); // java.io.InvalidClassException
        // tenantCode[1704421450] tenantId[94c63603-e487-4cb3-bc98-6129ec616722]
        Pattern TCODE_PATTERN = Pattern.compile("tenantCode\\[(\\d+)\\]");
        Pattern TID_PATTERN = Pattern.compile("tenantId\\[(.*?)\\]");

        Joiner JOINER = Joiner.on("");
        Splitter COMMA_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
    }

    @Data @AllArgsConstructor
    private static class ProcessBean {
        private final BlackcatReqSender sender;
        private final String[] commands;
        private final String logger;
        private final long ignoreMillisBefore;

        private Process process;
        private BufferedReader bufferedReader;
        private LineReader lineReader;

        private EvictingQueue<Object> evictingQueue = EvictingQueue.create(10);
        private List<String> exceptionStack = new ArrayList<>();

        public ProcessBean(BlackcatReqSender sender, String[] commands,
                           String logger, long ignoreMillisBefore) {
            this.sender = sender;
            this.commands = commands;
            this.logger = logger;
            this.ignoreMillisBefore = ignoreMillisBefore;

            createLineReader(commands);
        }

        public void reset() {
            try {
                process.destroy();
                IOUtils.close(bufferedReader);

                evictingQueue.clear();
                exceptionStack.clear();
                createLineReader(commands);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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
                ex.printStackTrace();
                reset();
            }

            detectException(lastNormalLine);
        }

        private void detectException(String lastNormalLine) {
            try {
                if (exceptionStack.isEmpty()) return;

                evictingQueue.add(Consts.JOINER.join(exceptionStack));
                findException(lastNormalLine);
                exceptionStack.clear();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void findException(String lastNormalLine) {
            val exceptionNames = createExceptionNames(lastNormalLine);
            if (exceptionNames.isEmpty()) return;
            if (isExceptionConfigIgnored(exceptionNames)) return;

            val req = createBlackcatLogException(lastNormalLine, exceptionNames);
            if (req.isPresent()) {
                log.warn("found exception in log:{}", req.get());
                sender.send(req.get());
            }
        }

        private String createExceptionNames(String lastNormalLine) {
            val exceptionNamesBuilder = new StringBuilder();

            List<String> stack = new ArrayList<>();
            stack.add(lastNormalLine);
            stack.addAll(exceptionStack);

            for (val line : stack) {
                val matcher = Consts.EXCEPTION_PATTERN.matcher(line);
                if (matcher.find()) {
                    exceptionNamesBuilder.append(line);
                }
            }

            return exceptionNamesBuilder.toString();
        }

        private boolean isExceptionConfigIgnored(String exceptionNames) {
            val miner = new Miner().getMiner("blackcat", "log.exception");
            val ignoreContains = miner.getString("ignore.contains");
            if (StringUtils.isEmpty(ignoreContains)) return false;

            for (String ignoreContain : Consts.COMMA_SPLITTER.split(ignoreContains)) {
                if (exceptionNames.contains(ignoreContain)) {
                    return true;
                }
            }

            return false;
        }

        private Optional<BlackcatMsg.BlackcatReq> createBlackcatLogException(String lastLine, String exceptionNames) {
            val tcode = findPattern(lastLine, Consts.TCODE_PATTERN);
            val tid = findPattern(lastLine, Consts.TID_PATTERN);
            val timestamp = findPattern(lastLine, Consts.NORMAL_LINE_PATTERN);

            // 在日志做CopyTruncate时，会导致日志文件重新扫描，此时捕获的日志时间戳与当前时间距离较远
            // 如果发现日志时间在1个小时之前，则忽略此异常日志
            if (ignoreMillisBefore > 0) {
                try {
                    val tt = DateTime.parse(timestamp, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                    if (System.currentTimeMillis() - tt.getMillis() > ignoreMillisBefore) {
                        evictingQueue.clear();
                        return Optional.empty();
                    }
                } catch (IllegalArgumentException ex) {
                    // ignore parse exception
                }
            }

            val build = BlackcatMsg.BlackcatLogException.newBuilder()
                    .setLogger(logger)
                    .setTcode(tcode)
                    .setTid(tid)
                    .setExceptionNames(exceptionNames)
                    .setTimestamp(timestamp)
                    .setContextLogs(Consts.JOINER.join(evictingQueue));
            evictingQueue.clear();

            return Optional.of(BlackcatMsg.BlackcatReq.newBuilder()
                    .setBlackcatReqHead(Blackcats.buildHead(BlackcatMsg.BlackcatReqHead.ReqType.BlackcatLogException))
                    .setBlackcatLogException(build)
                    .build());
        }

        private String findPattern(String line, Pattern pattern) {
            if (line == null) return "unknown";

            val matcher = pattern.matcher(line);
            return matcher.find() ? matcher.group(1) : "unknown";
        }
    }
}
