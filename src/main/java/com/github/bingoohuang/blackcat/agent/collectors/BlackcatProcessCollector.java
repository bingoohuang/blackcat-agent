package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.agent.utils.Utils;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMsgHead.MsgType;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMsgReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatProcess;
import com.google.common.base.Joiner;
import org.gridkit.lab.sigar.SigarFactory;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.ptql.ProcessFinder;

public class BlackcatProcessCollector
        implements BlackcatCollector<BlackcatMsgReq> {

    @Override
    public BlackcatMsgReq collect() {
        BlackcatProcess.Builder builder;
        builder = BlackcatProcess.newBuilder();

        try {
            ps("java", builder);
        } catch (SigarException e) {
            throw new RuntimeException(e);
        }
        return BlackcatMsgReq.newBuilder()
                .setHead(Utils.buildHead(MsgType.BlackcatProcess))
                .setProcess(builder).build();
    }


    private static void ps(
            String argsContains,
            BlackcatProcess.Builder builder
    ) throws SigarException {
        SigarProxy sigar = SigarFactory.newSigar();
        long[] pids = ProcessFinder.find(sigar,
                "Args.*.ct=" + argsContains);

        Joiner joiner = Joiner.on(' ');
        for (long pid : pids) {
            String[] procArgs = sigar.getProcArgs(pid);
            ProcMem procMem = sigar.getProcMem(pid);

            builder.addProc(BlackcatProcess.Proc.newBuilder()
                    .setPid(pid)
                    .setArgs(joiner.join(procArgs))
                    .setRes(procMem.getResident())
                    .build());
        }
    }

}
