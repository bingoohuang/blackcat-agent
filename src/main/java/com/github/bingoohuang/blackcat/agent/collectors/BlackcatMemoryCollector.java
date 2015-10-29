package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.agent.utils.Utils;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMemory;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Memory;

public class BlackcatMemoryCollector implements BlackcatCollector {

    @Override
    public BlackcatReq collect() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        Memory memory = hardware.getMemory();

        BlackcatMemory.Builder builder = BlackcatMemory.newBuilder()
                .setTotal(memory.getTotal())
                .setAvailable(memory.getAvailable());

        return BlackcatReq.newBuilder()
                .setBlackcatReqHead(Utils.buildHead(ReqType.BlackcatMemory))
                .setBlackcatMemory(builder).build();
    }
}
