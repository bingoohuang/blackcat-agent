package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.agent.utils.Utils;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatProcess;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatWarnConfig;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatWarnConfig.BlackcatWarnProcess;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import org.gridkit.lab.sigar.SigarFactory;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.ptql.ProcessFinder;

import java.util.List;

public class BlackcatProcessCollector implements BlackcatCollector {
    @Override
    public Optional<BlackcatReq> collect() {
        BlackcatProcess.Builder builder;
        builder = BlackcatProcess.newBuilder();

        try {
            if (warnProcesses != null) {
                for (BlackcatWarnProcess warnProcess : warnProcesses) {
                    ps(warnProcess, builder);
                }
            }
        } catch (SigarException e) {
            throw new RuntimeException(e);
        }

        if (builder.getProcList().isEmpty()) return Optional.absent();

        BlackcatReq blackcatReq = BlackcatReq.newBuilder()
                .setBlackcatReqHead(Utils.buildHead(ReqType.BlackcatProcess))
                .setBlackcatProcess(builder).build();

        return Optional.of(blackcatReq);
    }

    SigarProxy sigar = SigarFactory.newSigar();

    private void ps(
            BlackcatWarnProcess warnProcess,
            BlackcatProcess.Builder builder
    ) throws SigarException {
        // Process Table Query Language: https://support.hyperic.com/display/SIGAR/PTQL
        StringBuilder ptql = new StringBuilder();
        for (String processKey : warnProcess.getProcessKeysList()) {
            if (ptql.length() > 0) ptql.append(',');
            ptql.append("Args.*.ct=").append(processKey);
        }

        Joiner joiner = Joiner.on(' ');
        for (long pid : ProcessFinder.find(sigar, ptql.toString())) {
            builder.addProc(BlackcatProcess.Proc.newBuilder()
                    .setPid(pid)
                    .setArgs(joiner.join(sigar.getProcArgs(pid)))
                    .setRes(sigar.getProcMem(pid).getResident())
                    .setStartTime(sigar.getProcCpu(pid).getStartTime())
                    .setName(warnProcess.getProcessName())
                    .build());
        }
    }


    volatile List<BlackcatWarnProcess> warnProcesses;

    @Subscribe
    public void configRegister(BlackcatWarnConfig blackcatWarnConfig) {
        warnProcesses = blackcatWarnConfig.getBlackcatWarnProcessList();
    }

}
