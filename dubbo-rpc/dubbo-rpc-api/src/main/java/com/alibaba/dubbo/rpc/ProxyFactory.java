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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * ProxyFactory. (API/SPI, Singleton, ThreadSafe)
 *
 * 通过该代理工厂返回服务接口的代理对象，默认使用javassist动态代理的技术实现代理
 */
@SPI("javassist")
public interface ProxyFactory {

    /**
     * 服务导出时会调用该方法：根据入参将要暴露的服务封装为一个{@link Invoker}对象
     *
     * @param <T>
     * @param proxy     一般这个代理对象就是服务接口的实现对象，比如：com.alibaba.dubbo.demo.provider.DemoServiceImpl
     * @param type      要代理的服务接口类型, 比如：com.alibaba.dubbo.demo.DemoService
     * @param url       要暴露的 URL
     * @return invoker
     */
    @Adaptive({Constants.PROXY_KEY})
    <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException;

    /**
     * 服务导入的时候，会调用该方法创建一个代理服务的接口实例。
     * 该方法创建一个代理对象, T表示要调用的服务接口, Invoker封装了调用那台机器的那个服务，并且封装了入参是什么，出参是什么
     *
     * @param invoker
     * @return proxy
     */
    @Adaptive({Constants.PROXY_KEY})
    <T> T getProxy(Invoker<T> invoker) throws RpcException;



}