package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.google.common.base.Optional;
import lombok.val;
import oshi.SystemInfo;

import static com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;

public class BlackcatFileStoresCollector
        implements BlackcatCollector {

    @Override
    public Optional<BlackcatReq> collect() {
        val systemInfo = new SystemInfo();
        val hardware = systemInfo.getHardware();

        val builder = BlackcatMsg.BlackcatFileStores.newBuilder();

        for (val osFileStore : hardware.getFileStores()) {
            val fileStore = BlackcatMsg.BlackcatFileStores.FileStore.newBuilder()
                    .setName(osFileStore.getName())
                    .setDescription(osFileStore.getDescription())
                    .setTotal(osFileStore.getTotalSpace())
                    .setUsable(osFileStore.getUsableSpace())
                    .build();

            builder.addFileStore(fileStore);
        }


        val blackcatReq = BlackcatReq.newBuilder()
                .setBlackcatReqHead(Blackcats.buildHead(ReqType.BlackcatFileStores))
                .setBlackcatFileStores(builder).build();

        return Optional.of(blackcatReq);
    }
}
