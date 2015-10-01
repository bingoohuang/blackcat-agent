package com.github.bingoohuang.blackcat.agent;

import com.github.bingoohuang.blackcat.agent.collectors.*;
import com.github.bingoohuang.blackcat.sdk.netty.BlackcatClient;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMsgReq;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
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
        }

        while (true) {
            for (BlackcatCollector collector : collectors) {
                BlackcatMsgReq req = collector.collect();
                if (options.has("send")) client.send(req);
                if (options.has("print")) System.out.println(req);
            }

            Blackcats.sleep(1, TimeUnit.MINUTES);
        }
    }

}
