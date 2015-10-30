package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.google.common.base.Optional;

public interface BlackcatCollector {
    Optional<BlackcatReq> collect();
}
