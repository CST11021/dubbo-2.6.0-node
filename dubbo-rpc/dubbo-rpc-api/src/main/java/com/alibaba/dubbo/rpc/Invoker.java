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
package com.alibaba.dubbo.rpc;

import com.alibaba.dubbo.common.Node;

/**
 * 可以通过该接口返回服务的接口类型或返回目标服务的调用结果
 * Invoker. (API/SPI, Prototype, ThreadSafe)
 *
 * @see com.alibaba.dubbo.rpc.Protocol#refer(Class, com.alibaba.dubbo.common.URL)
 * @see com.alibaba.dubbo.rpc.InvokerListener
 * @see com.alibaba.dubbo.rpc.protocol.AbstractInvoker
 */
public interface Invoker<T> extends Node {

    /**
     * get service interface.
     * 获取服务接口类型
     *
     * @return service interface.
     */
    Class<T> getInterface();

    /**
     * invoke.
     * 调用{@link Invocation}中封装的目标方法
     *
     * @see Invocation 仅封装了调用方法的方法名、方法入参类型和方法入参等信息，并没有封装目标对象，目标对象的载体在
     * {@link Invoker}接口的实现类中
     *
     * @param invocation
     * @return result           返回服务方法的执行结果
     * @throws RpcException
     */
    Result invoke(Invocation invocation) throws RpcException;

}