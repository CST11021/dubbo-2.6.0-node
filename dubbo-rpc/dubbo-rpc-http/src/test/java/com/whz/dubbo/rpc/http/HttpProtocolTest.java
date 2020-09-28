package com.whz.dubbo.rpc.http;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.protocol.http.HttpProtocol;
import org.junit.Test;

/**
 * @Author: wanghz
 * @Date: 2020/9/27 11:06 AM
 */
public class HttpProtocolTest {

    URL url = new URL("http", "localhost", 80);

    Protocol protocol = new HttpProtocol();

    @Test
    public void t() throws NoSuchMethodException {

        Invoker invoker = protocol.refer(HelloService.class, url);

        Invocation invocation = new RpcInvocation();
        ((RpcInvocation) invocation).setMethodName("sayHello");
        ((RpcInvocation) invocation).setArguments(new Object[] {"张三"});

        Result result = invoker.invoke(invocation);

        result.getValue();

    }

    public interface HelloService {

        String sayHello(String name);

    }

}
