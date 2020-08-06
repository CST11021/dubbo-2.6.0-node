package com.alibaba.dubbo.demo.consumer.filter;

import com.alibaba.dubbo.rpc.*;

import java.util.Map;
import java.util.UUID;

public class ConsumerTraceFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Map<String, String> stringStringMap = invocation.getAttachments();
        // 设置参数
        stringStringMap.put("patentId","0");
        stringStringMap.put("traceId",UUID.randomUUID().toString());
        System.out.println("生成tradeId:"+stringStringMap.get("traceId"));
        return invoker.invoke(invocation);
    }
}