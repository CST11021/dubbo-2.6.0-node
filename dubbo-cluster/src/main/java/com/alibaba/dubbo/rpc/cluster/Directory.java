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

import com.alibaba.dubbo.common.Node;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.List;

/**
 * 在Dubbo中，通过Router进行过滤后，会使用Directory封装多个服务提供者，最后通过负载均衡确定一个要用于本次调用的目标服务
 *
 * Directory. (SPI, Prototype, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Directory_service">Directory Service</a>
 *
 * @see com.alibaba.dubbo.rpc.cluster.Cluster#join(Directory)
 */
public interface Directory<T> extends Node {

    /**
     * 表示本次调用目标服务接口
     *
     * @return service type.
     */
    Class<T> getInterface();

    /**
     * 根据请求的方法签名及入参，返回多个{@link Invoker}对象（可能存在多个服务提供者）。 {@link Cluster}会将 Directory 中的
     * 多个 Invoker 伪装成一个 Invoker, 对上层透明，包含集群的容错机制。
     *
     * 通过Router进行过滤后，会使用Directory封装多个服务提供者，最后通过负载均衡确定一个要用于本次调用的目标服务
     *
     * @return invokers
     */
    List<Invoker<T>> list(Invocation invocation) throws RpcException;

}