package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatNetStat;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.google.common.base.Optional;
import org.gridkit.lab.sigar.SigarFactory;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;

public class BlackcatNetstatCollector implements BlackcatCollector {
    @Override
    public Optional<BlackcatReq> collect() throws SigarException {
        SigarProxy sigar = SigarFactory.newSigar();
        NetStat netStat = sigar.getNetStat();
        BlackcatNetStat.Builder builder = BlackcatNetStat.newBuilder();
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

        BlackcatReq blackcatReq = BlackcatReq.newBuilder()
                .setBlackcatReqHead(Blackcats.buildHead(ReqType.BlackcatNetStat))
                .setBlackcatNetStat(builder).build();
        return Optional.of(blackcatReq);
    }

}
