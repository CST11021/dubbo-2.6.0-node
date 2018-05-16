/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.cluster;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.List;

/**
 * Router. (SPI, Prototype, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Routing">Routing</a>
 *
 * @see com.alibaba.dubbo.rpc.cluster.Cluster#join(Directory)
 * @see com.alibaba.dubbo.rpc.cluster.Directory#list(Invocation)
 */
public interface Router extends Comparable<Router> {

    /**
     * 获取配置的路由规则，比如：host = 192.168.85.1 =>  host = 10.20.3.3
     *
     * @return url
     */
    URL getUrl();

    /**
     * Router 服务路由， 根据路由规则从多个 Invoker 中选出一个子集
     * AbstractDirectory 是所有目录服务实现的上层抽象，它在 list 列举出所有 invokers 后，会在通过 Router 服务进行路由过滤。
     *
     * 注意与负载均衡的区别：LoadBalance负载均衡，负责从多个 Invokers中选出具体的一个Invoker用于本次调用，调用过程中包含
     * 了负载均衡的算法，调用失败后需要重新选择
     *
     * @param invokers          表示所有的服务提供者的Invoker对象
     * @param url               消费者调用的URL信息，例如：consumer://192.168.85.1/com.foo.BarService
     * @param invocation        表示本次调用的方法入参信息
     * @return routed invokers  根据路由规则，返回该消费者所有可以调用的Invoker
     * @throws RpcException
     */
    <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException;

}