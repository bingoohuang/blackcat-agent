package com.github.bingoohuang.blackcat.agent;

import com.github.bingoohuang.blackcat.agent.collectors.*;
import com.github.bingoohuang.blackcat.sdk.netty.BlackcatClient;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMsgReq;
import com.github.bingoohuang.blackcat.sdk.utils.BlackCats;
import com.google.common.collect.ImmutableList;
import org.hyperic.sigar.SigarException;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws SigarException {
        List<BlackcatCollector> collectors
                = ImmutableList.<BlackcatCollector>of(
                new BlackcatMemoryCollector(),
                new BlackcatLoadCollector(),
                new BlackcatFileStoresCollector(),
                new BlackcatProcessCollector()
        );

        while (true) {
            for (BlackcatCollector collector : collectors) {
                BlackcatMsgReq req = collector.collect();
                BlackcatClient.send(req);
            }

            BlackCats.sleep(1, TimeUnit.MINUTES);
        }
    }

}
