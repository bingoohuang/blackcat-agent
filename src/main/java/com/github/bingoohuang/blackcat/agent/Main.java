package com.github.bingoohuang.blackcat.agent;

import com.github.bingoohuang.blackcat.agent.collectors.*;
import com.github.bingoohuang.blackcat.sdk.netty.BlackcatClient;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.hyperic.sigar.SigarException;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws SigarException {
        OptionParser parser = new OptionParser();
        parser.accepts("print");
        parser.accepts("send");

        OptionSet options = parser.parse(args);

        List<BlackcatCollector> collectors
                = ImmutableList.<BlackcatCollector>of(
                new BlackcatMemoryCollector(),
                new BlackcatLoadCollector(),
                new BlackcatFileStoresCollector(),
                new BlackcatProcessCollector()
        );

        BlackcatClient client = null;

        if (options.has("send")) {
            client = new BlackcatClient();
            client.connect();

            for (BlackcatCollector collector : collectors) {
                client.register(collector);
            }
        }

        while (true) {
            for (BlackcatCollector collector : collectors) {
                Optional<BlackcatReq> req = collector.collect();
                if (!req.isPresent()) continue;

                if (options.has("send")) client.send(req.get());
                if (options.has("print")) System.out.println(req.get());
            }

            Blackcats.sleep(1, TimeUnit.MINUTES);
        }
    }

}
