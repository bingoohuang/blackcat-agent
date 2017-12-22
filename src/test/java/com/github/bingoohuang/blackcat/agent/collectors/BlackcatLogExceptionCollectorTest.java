package com.github.bingoohuang.blackcat.agent.collectors;

import com.github.bingoohuang.blackcat.sdk.netty.BlackcatReqSender;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class BlackcatLogExceptionCollectorTest {
    @Test @SneakyThrows
    public void test() {
        BlackcatReqSender client = req -> System.out.println(req);

        File a1 = File.createTempFile("aaa", "a1");
        a1.deleteOnExit();
        File b1 = File.createTempFile("bbb", "b1");
        b1.deleteOnExit();

        String logFiles = "a1:" + a1.getAbsolutePath() + ",b1:" + b1.getAbsolutePath();
        val collector = new BlackcatLogExceptionCollector(client, logFiles, 10);
        collector.start();


        Files.append("2017-11-19 23:54:53.026 ali-hd2-hi-app01 [http-nio-8026-exec-8] INFO  tenantCode[1704421450] tenantId[94c63603-e487-4cb3-bc98-6129ec616722] com.raiyee.hi.base.authc.filter.AutoAuthcFilter:103 - 当前链接使用snsapi_base方式授权,获取用户信息\n", a1, Charsets.UTF_8);
        Files.append("2017-11-19 23:54:53.027 ali-hd2-hi-app01 [http-nio-8026-exec-8] INFO  tenantCode[1704421450] tenantId[94c63603-e487-4cb3-bc98-6129ec616722] com.raiyee.hi.api.cpbas.merchant.UserManagementApi:50 - spring rest client sync 616372a8-53dd-459d-b50e-2219459e39f1 request: POST http://127.0.0.1:18020/v1/user-management/get-user-by-code headers: body: code=001zrOc52UiNJK0RWhb52wk5d52zrOco&tid=94c63603-e487-4cb3-bc98-6129ec616722\n", a1, Charsets.UTF_8);
        Files.append("2017-11-19 23:54:53.162 ali-hd2-hi-app01 [http-nio-8026-exec-8] ERROR tenantCode[1704421450] tenantId[94c63603-e487-4cb3-bc98-6129ec616722] com.raiyee.hi.base.authc.filter.AutoAuthcFilter:53 - 自动登录失败\n", a1, Charsets.UTF_8);
        Files.append("com.github.bingoohuang.springrestclient.exception.RestException: 获取refreshAccessToken失败!\n" +
                "        at com.github.bingoohuang.springrestclient.utils.RestReq.processStatusExceptionMappings(RestReq.java:457) ~[spring-rest-client-0.0.19.jar:na]\n" +
                "        at com.github.bingoohuang.springrestclient.utils.RestReq.request(RestReq.java:336) ~[spring-rest-client-0.0.19.jar:na]\n" +
                "        at com.github.bingoohuang.springrestclient.utils.RestReq.post(RestReq.java:143) ~[spring-rest-client-0.0.19.jar:na]\n" +
                "        at com.raiyee.hi.api.cpbas.merchant.UserManagementApi$$BINGOOASM$$Impl.getUserByCode(Unknown Source) ~[na:na]\n" +
                "        at com.raiyee.hi.base.authc.enviroment.WxContext.getCpUserIfNecessary(WxContext.java:19) ~[classes/:na]\n", a1, Charsets.UTF_8);
        Files.append("", a1, Charsets.UTF_8);
        Files.append("", a1, Charsets.UTF_8);

        Files.append("2017-11-19 23:54:53.026 ali-hd2-hi-app01 [http-nio-8026-exec-8] INFO  tenantCode[99999999] tenantId[94c63603-e487-4cb3-bc98-6129ec616722] com.raiyee.hi.base.authc.filter.AutoAuthcFilter:103 - 当前链接使用snsapi_base方式授权,获取用户信息\n", b1, Charsets.UTF_8);
        Files.append("2017-11-19 23:54:53.027 ali-hd2-hi-app01 [http-nio-8026-exec-8] INFO  tenantCode[99999999] tenantId[94c63603-e487-4cb3-bc98-6129ec616722] com.raiyee.hi.api.cpbas.merchant.UserManagementApi:50 - spring rest client sync 616372a8-53dd-459d-b50e-2219459e39f1 request: POST http://127.0.0.1:18020/v1/user-management/get-user-by-code headers: body: code=001zrOc52UiNJK0RWhb52wk5d52zrOco&tid=94c63603-e487-4cb3-bc98-6129ec616722\n", b1, Charsets.UTF_8);
        Files.append("2017-11-19 23:54:53.162 ali-hd2-hi-app01 [http-nio-8026-exec-8] ERROR tenantCode[99999999] tenantId[94c63603-e487-4cb3-bc98-6129ec616722] com.raiyee.hi.base.authc.filter.AutoAuthcFilter:53 - 自动登录失败\n", b1, Charsets.UTF_8);
        Files.append("com.github.bingoohuang.springrestclient.exception.RestException: 获取refreshAccessToken失败!\n" +
                "        at com.github.bingoohuang.springrestclient.utils.RestReq.processStatusExceptionMappings(RestReq.java:457) ~[spring-rest-client-0.0.19.jar:na]\n" +
                "        at com.github.bingoohuang.springrestclient.utils.RestReq.request(RestReq.java:336) ~[spring-rest-client-0.0.19.jar:na]\n" +
                "        at com.github.bingoohuang.springrestclient.utils.RestReq.post(RestReq.java:143) ~[spring-rest-client-0.0.19.jar:na]\n" +
                "        at com.raiyee.hi.api.cpbas.merchant.UserManagementApi$$BINGOOASM$$Impl.getUserByCode(Unknown Source) ~[na:na]\n" +
                "        at com.raiyee.hi.base.authc.enviroment.WxContext.getCpUserIfNecessary(WxContext.java:19) ~[classes/:na]\n", b1, Charsets.UTF_8);
        Files.append("", b1, Charsets.UTF_8);
        Files.append("", b1, Charsets.UTF_8);

        val inputStream = Blackcats.classpathInputStream("test.log");
        val byteSource = new ByteSource() {
            public InputStream openStream() {
                return inputStream;
            }
        };

        String text = byteSource.asCharSource(Charsets.UTF_8).read();
        Files.append(text, a1, Charsets.UTF_8);

//        while (true) {
        Blackcats.sleep(1, TimeUnit.MINUTES);
//        }
    }
}