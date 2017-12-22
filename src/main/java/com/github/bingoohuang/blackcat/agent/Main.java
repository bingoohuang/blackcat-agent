package com.github.bingoohuang.blackcat.agent;

import com.github.bingoohuang.blackcat.agent.collectors.*;
import com.github.bingoohuang.blackcat.sdk.netty.BlackcatNettyClient;
import com.github.bingoohuang.blackcat.sdk.netty.BlackcatReqSender;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.google.common.collect.ImmutableList;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        val parser = new OptionParser();
        parser.accepts("print");
        parser.accepts("send");

        // eg: --logs=yoga:/home/app/tomcat/yoga-system/logs/catalina.out,et:/home/app/et-server/et-server.log
        val logs = parser.accepts("logs").withOptionalArg().ofType(String.class);
        val options = parser.parse(args);

        val collectors = ImmutableList.of(
                new BlackcatMemoryCollector(),
                new BlackcatLoadCollector(),
                new BlackcatFileStoresCollector(),
                new BlackcatProcessCollector()
//                new BlackcatFileSystemUsageCollector()
//                new BlackcatNetstatCollector(),
//                new BlackcatRedisInfoCollector()
        );

        boolean send = options.has("send");
        val client = createNettyClient(collectors, send);
        val sender = createSender(options, client, send);

        if (sender == null) {
            System.out.printf("at least one of --print or --send argument is required!");
            return;
        }

        val logsConfig = logs.value(options);
        if (StringUtils.isNotBlank(logsConfig)) {
            val logExceptionCollector = new BlackcatLogExceptionCollector(sender, logsConfig, 10);
            logExceptionCollector.start();
        }

        while (true) {
            for (val collector : collectors) {
                val req = collector.collect();
                if (!req.isPresent()) continue;

                sender.send(req.get());
            }

            Blackcats.sleep(1, TimeUnit.MINUTES);
        }
    }

    private static BlackcatNettyClient createNettyClient(ImmutableList<BlackcatCollector> collectors, boolean send) {
        if (!send) return null;

        val client = new BlackcatNettyClient();
        client.connect();

        for (val collector : collectors) {
            client.register(collector);
        }

        return client;
    }

    private static BlackcatReqSender createSender(OptionSet options, BlackcatNettyClient client, boolean send) {
        val print = options.has("print");

        if (print && send) {
            val nettyClient = client;
            return req -> {
                System.out.println(req);

                nettyClient.send(req);
            };
        } else if (print) {
            return req -> System.out.println(req);
        } else if (send) {
            val nettyClient = client;
            return req -> nettyClient.send(req);
        } else {
            return null;
        }
    }

}
