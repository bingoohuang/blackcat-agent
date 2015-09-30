package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.agent.utils.Utils;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMemory;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMsgHead.MsgType;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMsgReq;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Memory;

public class BlackcatMemoryCollector
        implements BlackcatCollector<BlackcatMsgReq> {

    @Override
    public BlackcatMsgReq collect() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        Memory memory = hardware.getMemory();

        BlackcatMemory.Builder builder = BlackcatMemory.newBuilder()
                .setTotal(memory.getTotal())
                .setAvailable(memory.getAvailable());

        return BlackcatMsgReq.newBuilder()
                .setHead(Utils.buildHead(MsgType.BlackcatMemory))
                .setMemory(builder).build();
    }
}
