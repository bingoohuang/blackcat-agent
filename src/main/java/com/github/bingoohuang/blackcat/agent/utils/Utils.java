package com.github.bingoohuang.blackcat.agent.utils;

import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;

public class Utils {
    public static BlackcatReqHead buildHead(BlackcatReqHead.ReqType reqType) {
        return BlackcatReqHead.newBuilder()
                .setHostname(Blackcats.getHostname())
                .setReqType(reqType)
                .setTimestamp(System.currentTimeMillis())
                .build();
    }
}
