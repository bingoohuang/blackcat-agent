package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.google.common.base.Optional;
import org.hyperic.sigar.SigarException;

public interface BlackcatCollector {
    Optional<BlackcatReq> collect() throws SigarException;
}
