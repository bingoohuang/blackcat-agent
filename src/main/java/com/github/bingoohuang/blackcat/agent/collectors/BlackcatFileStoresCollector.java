package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.agent.utils.Utils;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatFileStores;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSFileStore;

import static com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;

public class BlackcatFileStoresCollector
        implements BlackcatCollector {

    @Override
    public BlackcatReq collect() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();

        BlackcatFileStores.Builder builder;
        builder = BlackcatMsg.BlackcatFileStores.newBuilder();

        BlackcatFileStores.FileStore fileStore;
        for (OSFileStore osFileStore : hardware.getFileStores()) {
            fileStore = BlackcatMsg.BlackcatFileStores.FileStore.newBuilder()
                    .setName(osFileStore.getName())
                    .setDescription(osFileStore.getDescription())
                    .setTotal(osFileStore.getTotalSpace())
                    .setUsable(osFileStore.getUsableSpace())
                    .build();

            builder.addFileStore(fileStore);
        }


        return BlackcatReq.newBuilder()
                .setBlackcatReqHead(Utils.buildHead(ReqType.BlackcatFileStores))
                .setBlackcatFileStores(builder).build();
    }
}
