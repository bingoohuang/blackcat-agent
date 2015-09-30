package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg;

public interface BlackcatCollector<BlackcatMsgReq> {
    BlackcatMsg.BlackcatMsgReq collect();
}
