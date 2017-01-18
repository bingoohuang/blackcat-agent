package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMemory;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.google.common.base.Optional;
import lombok.val;
import oshi.SystemInfo;

public class BlackcatMemoryCollector implements BlackcatCollector {

    @Override
    public Optional<BlackcatReq> collect() {
        val systemInfo = new SystemInfo();
        val hardware = systemInfo.getHardware();
        val memory = hardware.getMemory();

        val builder = BlackcatMemory.newBuilder()
                .setTotal(memory.getTotal())
                .setAvailable(memory.getAvailable());

        val blackcatReq = BlackcatReq.newBuilder()
                .setBlackcatReqHead(Blackcats.buildHead(ReqType.BlackcatMemory))
                .setBlackcatMemory(builder).build();
        return Optional.of(blackcatReq);
    }
}
