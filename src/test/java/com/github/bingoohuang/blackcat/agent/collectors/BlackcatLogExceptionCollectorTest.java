package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.sdk.netty.BlackcatReqSender;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class BlackcatLogExceptionCollectorTest {
    @Test @SneakyThrows
    public void test() {
        BlackcatReqSender client = req -> System.out.println(req);

        val a1 = File.createTempFile("aaa", "a1");
        a1.deleteOnExit();
        val b1 = File.createTempFile("bbb", "b1");
        b1.deleteOnExit();

        val logFiles = "a1:" + a1.getAbsolutePath() + ",b1:" + b1.getAbsolutePath();
        val collector = new BlackcatLogExceptionCollector(client, logFiles, 10, 0);
        collector.start();

        appendLog("testaaa.log", a1);
        appendLog("testbbb.log", b1);

//        while (true) {
        Blackcats.sleep(1, TimeUnit.MINUTES);
//        }
    }

    private void appendLog(String classPathFile, File file) throws IOException {
        val byteSource = new ByteSource() {
            public InputStream openStream() {
                return Blackcats.classpathInputStream(classPathFile);
            }
        };
        String content = byteSource.asCharSource(Charsets.UTF_8).read();
        Files.append(content, file, Charsets.UTF_8);
    }
}