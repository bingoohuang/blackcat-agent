package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatLoad;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.google.common.base.Optional;
import org.gridkit.lab.sigar.SigarFactory;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;

public class BlackcatLoadCollector implements BlackcatCollector {

    @Override
    public Optional<BlackcatReq> collect() {
        SigarProxy sigar = SigarFactory.newSigar();
        try {
            double[] loadAverage = sigar.getLoadAverage();
            int cpuNum = sigar.getCpuList().length;

            BlackcatLoad.Builder builder = BlackcatLoad.newBuilder()
                    .setCpuNum(cpuNum)
                    .setOneMinAvg((float) loadAverage[0])
                    .setFiveMinsAvg((float) loadAverage[1])
                    .setFifteenMinsAvg((float) loadAverage[2]);

            BlackcatReq blackcatReq = BlackcatReq.newBuilder()
                    .setBlackcatReqHead(Blackcats.buildHead(ReqType.BlackcatLoad))
                    .setBlackcatLoad(builder).build();
            return Optional.of(blackcatReq);

        } catch (SigarException e) {
            throw new RuntimeException(e);
        }
    }
}
