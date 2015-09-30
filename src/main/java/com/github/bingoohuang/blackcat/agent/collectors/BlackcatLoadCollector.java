package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.agent.utils.Utils;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatLoad;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMsgHead.MsgType;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMsgReq;
import org.gridkit.lab.sigar.SigarFactory;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;

public class BlackcatLoadCollector
        implements BlackcatCollector<BlackcatMsgReq> {

    @Override
    public BlackcatMsgReq collect() {
        SigarProxy sigar = SigarFactory.newSigar();
        try {
            double[] loadAverage = sigar.getLoadAverage();
            int cpuNum = sigar.getCpuList().length;

            BlackcatLoad.Builder builder = BlackcatLoad.newBuilder()
                    .setCpuNum(cpuNum)
                    .setOneMinAvg(loadAverage[0])
                    .setFiveMinsAvg(loadAverage[1])
                    .setFifteenMinsAvg(loadAverage[2]);

            return BlackcatMsgReq.newBuilder()
                    .setHead(Utils.buildHead(MsgType.BlackcatLoad))
                    .setLoad(builder).build();

        } catch (SigarException e) {
            throw new RuntimeException(e);
        }
    }
}
