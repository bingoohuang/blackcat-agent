package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatLoad;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.val;
import org.gridkit.lab.sigar.SigarFactory;
import org.hyperic.sigar.ProcUtil;
import org.hyperic.sigar.SigarException;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    @SneakyThrows
    public static List<BlackcatLoad.TopProcess> topN() {
        val sigar = SigarFactory.newSigar();

        val result = Lists.<BlackcatLoad.TopProcess>newArrayList();
        for (int watchDog = 0; result.isEmpty() && watchDog < 10; ++watchDog) {
            for (val processId : sigar.getProcList()) {
                try {
                    val procCpu = sigar.getProcCpu(processId);
                    val name = ProcUtil.getDescription(sigar, processId);
                    if (procCpu.getPercent() > 0.001f) {
                        result.add(BlackcatLoad.TopProcess.newBuilder()
                                .setPid(processId)
                                .setName(name)
                                .setCpuPercent(procCpu.getPercent())
                                .build());
                    }
                } catch (SigarException e) {
                    //for denied access on some pid
                }
            }
        }

        Collections.sort(result, new Comparator<BlackcatLoad.TopProcess>() { // sort in desc order
            @Override public int compare(BlackcatLoad.TopProcess o1, BlackcatLoad.TopProcess o2) {
                return Double.compare(o2.getCpuPercent(), o1.getCpuPercent());
            }
        });

        return result;
    }
}
