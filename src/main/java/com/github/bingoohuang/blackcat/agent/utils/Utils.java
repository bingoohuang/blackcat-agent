package com.github.bingoohuang.blackcat.agent.utils;

import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMsgHead;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;

public class Utils {
    public static BlackcatMsgHead buildHead(BlackcatMsgHead.MsgType msgType) {
        return BlackcatMsgHead.newBuilder()
                .setHostname(Blackcats.getHostname())
                .setMsgType(msgType)
                .setTimestamp(System.currentTimeMillis())
                .build();
    }
}
