package com.github.bingoohuang.blackcat.agent.collectors;

import com.alibaba.fastjson.JSON;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatJSON;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.hyperic.sigar.SigarException;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BlackcatRedisInfoCollector implements BlackcatCollector {
    @Override
    public Optional<BlackcatReq> collect() throws SigarException {
        Optional<String> info = redisInfo();
        if (!info.isPresent()) return Optional.absent();

        BlackcatJSON.Builder builder = BlackcatJSON.newBuilder()
                .setJson(info.get())
                .setSchema("RedisInfo");

        BlackcatReq blackcatReq = BlackcatReq.newBuilder()
                .setBlackcatReqHead(Blackcats.buildHead(ReqType.BlackcatJSON))
                .setBlackcatJSON(builder).build();
        return Optional.of(blackcatReq);
    }

    static Pattern keyValuePattern = Pattern.compile("(\\w+):(\\w+)");

    /*
    执行redis的info命令,采集命令输出的key-value.
    ~/g/blackcat-sdk > echo -e 'info\r\nquit\r\n' | curl -s telnet://localhost:6379
    $1895
    # Server
    redis_version:2.8.19
    redis_git_sha1:00000000
    redis_git_dirty:0
    redis_build_id:a2aec8d93a0fa8d5
    redis_mode:standalone
    os:Darwin 15.2.0 x86_64
    arch_bits:64
    ...
     */
    private Optional<String> redisInfo() {
        String script = "echo -e 'info\r\nquit\r\n' " +
                "| curl -s telnet://localhost:6379";
        String result = Blackcats.runShellScript(script);

        Map<String, String> infoResult = Maps.newHashMap();
        Matcher m = keyValuePattern.matcher(result);
        while (m.find()) infoResult.put(m.group(1), m.group(2));

        if (infoResult.isEmpty()) return Optional.absent();

        return Optional.of(JSON.toJSONString(infoResult));
    }

}
