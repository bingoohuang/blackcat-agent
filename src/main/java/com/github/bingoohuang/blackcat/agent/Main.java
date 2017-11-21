package com.github.bingoohuang.blackcat.agent;

import com.github.bingoohuang.blackcat.agent.collectors.BlackcatFileStoresCollector;
import com.github.bingoohuang.blackcat.agent.collectors.BlackcatLoadCollector;
import com.github.bingoohuang.blackcat.agent.collectors.BlackcatMemoryCollector;
import com.github.bingoohuang.blackcat.agent.collectors.BlackcatProcessCollector;
import com.github.bingoohuang.blackcat.sdk.netty.BlackcatNettyClient;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.google.common.collect.ImmutableList;
import joptsimple.OptionParser;
import lombok.val;
import org.hyperic.sigar.SigarException;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws SigarException {
        val parser = new OptionParser();
        parser.accepts("print");
        parser.accepts("send");

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

        BlackcatNettyClient client = null;

        if (options.has("send")) {
            client = new BlackcatNettyClient();
            client.connect();

            for (val collector : collectors) {
                client.register(collector);
            }
        }

        while (true) {
            for (val collector : collectors) {
                val req = collector.collect();
                if (!req.isPresent()) continue;

                if (client != null) client.send(req.get());
                if (options.has("print")) System.out.println(req.get());
            }

            Blackcats.sleep(1, TimeUnit.MINUTES);
        }
    }

}
