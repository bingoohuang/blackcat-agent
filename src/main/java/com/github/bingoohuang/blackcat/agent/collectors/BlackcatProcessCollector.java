package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatProcess;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatWarnConfig;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatWarnConfig.BlackcatWarnProcess;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import lombok.SneakyThrows;
import lombok.val;
import org.hyperic.sigar.ptql.ProcessFinder;

import java.util.ArrayList;
import java.util.List;

public class BlackcatProcessCollector implements BlackcatCollector {
    @Override @SneakyThrows
    public Optional<BlackcatReq> collect() {
        val builder = BlackcatProcess.newBuilder();

        if (warnProcesses != null) {
            val pids = new ArrayList<Long>();
            for (val warnProcess : warnProcesses) {
                // ct - Contains value (substring)
                ps(pids, warnProcess, builder, "Args.*.ct="); // Command line argument passed to the process
                ps(pids, warnProcess, builder, "State.Name.ct="); // Base name of the process executable
                // Exe.Name - Full path name of the process executable THIS DO NOT WORK!quit
            }
        }

        val blackcatReq = BlackcatReq.newBuilder()
                .setBlackcatReqHead(Blackcats.buildHead(ReqType.BlackcatProcess))
                .setBlackcatProcess(builder).build();

        return Optional.of(blackcatReq);
    }

    @SneakyThrows
    private void ps(List<Long> pids, BlackcatWarnProcess warnProcess,
                    BlackcatProcess.Builder builder, String queryCondition
    ) {
        // Process Table Query Language: https://support.hyperic.com/display/SIGAR/PTQL
        val ptql = new StringBuilder();
        for (val processKey : warnProcess.getProcessKeysList()) {
            if (ptql.length() > 0) ptql.append(',');
            ptql.append(queryCondition).append(processKey);
        }

        val joiner = Joiner.on(' ');

        val sigar = SigarSingleton.SIGAR;

        for (val pid : ProcessFinder.find(sigar, ptql.toString())) {
            if (pids.contains(pid)) continue;

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
