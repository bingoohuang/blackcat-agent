package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatNetStat;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.google.common.base.Optional;
import lombok.SneakyThrows;
import lombok.val;
import org.gridkit.lab.sigar.SigarFactory;

public class BlackcatNetstatCollector implements BlackcatCollector {
    @Override @SneakyThrows
    public Optional<BlackcatReq> collect() {
        val sigar = SigarFactory.newSigar();
        val netStat = sigar.getNetStat();
        val builder = BlackcatNetStat.newBuilder();
        builder.setAllInboundTotal(netStat.getAllInboundTotal())
                .setAllOutboundTotal(netStat.getAllOutboundTotal())
                .setTcpBound(netStat.getTcpBound())
                .setTcpClose(netStat.getTcpClose())
                .setTcpCloseWait(netStat.getTcpCloseWait())
                .setTcpClosing(netStat.getTcpClosing())
                .setTcpEstablished(netStat.getTcpEstablished())
                .setTcpFinWait1(netStat.getTcpFinWait1())
                .setTcpFinWait2(netStat.getTcpFinWait2())
                .setTcpIdle(netStat.getTcpIdle())
                .setTcpInboundTotal(netStat.getTcpInboundTotal())
                .setTcpLastAck(netStat.getTcpLastAck())
                .setTcpListen(netStat.getTcpListen())
                .setTcpOutboundTotal(netStat.getTcpOutboundTotal())
                .setTcpSynRecv(netStat.getTcpSynRecv())
                .setTcpSynSent(netStat.getTcpSynSent())
                .setTcpTimeWait(netStat.getTcpTimeWait());

        val blackcatReq = BlackcatReq.newBuilder()
                .setBlackcatReqHead(Blackcats.buildHead(ReqType.BlackcatNetStat))
                .setBlackcatNetStat(builder).build();
        return Optional.of(blackcatReq);
    }

}
