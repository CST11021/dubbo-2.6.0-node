package com.alibaba.dubbo.demo.provider.filter;

import com.alibaba.dubbo.rpc.*;

import java.util.Map;

public class ProviderTraceFilter implements Filter {

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Map<String, String> stringStringMap = invocation.getAttachments();
        // 接收参数
        System.out.println("收到：patentId:"+stringStringMap.get("patentId"));
        System.out.println("收到：tradeId:"+stringStringMap.get("traceId"));
        return invoker.invoke(invocation);
    }
}