package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatLoad;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.google.common.base.Optional;
import lombok.SneakyThrows;
import lombok.val;
import org.gridkit.lab.sigar.SigarFactory;

public class BlackcatLoadCollector implements BlackcatCollector {

    @Override @SneakyThrows
    public Optional<BlackcatReq> collect() {
        val sigar = SigarFactory.newSigar();
        double[] loadAverage = sigar.getLoadAverage();
        int cpuNum = sigar.getCpuList().length;

        val builder = BlackcatLoad.newBuilder()
                .setCpuNum(cpuNum)
                .setOneMinAvg((float) loadAverage[0])
                .setFiveMinsAvg((float) loadAverage[1])
                .setFifteenMinsAvg((float) loadAverage[2]);

        val blackcatReq = BlackcatReq.newBuilder()
                .setBlackcatReqHead(Blackcats.buildHead(ReqType.BlackcatLoad))
                .setBlackcatLoad(builder).build();
        return Optional.of(blackcatReq);
    }
}
