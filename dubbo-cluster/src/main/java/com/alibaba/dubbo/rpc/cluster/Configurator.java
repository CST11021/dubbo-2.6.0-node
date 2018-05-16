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

/**
 * Configurator. (SPI, Prototype, ThreadSafe)
 * 配置规则实际上是在生成 invoker 的过程中对 url 进行改写
 * 目前包含两种规则：AbsentConfigurator 和 OverrideConfigurator；前者是如果缺少项，则新增；而后者是直接覆盖；
 *
 * 具体是在 RegistryDirectory#mergeUrl() 函数中用到；
 *
 * 另外，根据 dubbo 文档描述如下：
     向注册中心写入动态配置覆盖规则：(通常由监控中心或治理中心的页面完成)
     RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
     Registry registry = registryFactory.getRegistry(URL.valueOf("zookeeper://10.20.153.10:2181"));
     registry.register(URL.valueOf("override://0.0.0.0/com.foo.BarService?category=configurators&dynamic=false&application=foo&timeout=1000"));

 */
public interface Configurator extends Comparable<Configurator> {

    /**
     * 获取这个配置规则，例如：“override://foo@0.0.0.0/com.foo.BarService?timeout=200”，表示该所有该服务接口的超时时间为200
     *
     * @return configurator url.
     */
    URL getUrl();

    /**
     * 如果使用的覆盖配置，对应配置规则是：override://foo@0.0.0.0/com.foo.BarService?timeout=200
     * 入参是：dubbo://10.20.153.10:20880/com.foo.BarService?application=foo&side=consumer
     * 则会返回：dubbo://10.20.153.10:20880/com.foo.BarService?application=foo&side=consumer&timeout=200
     *
     * @param url   表示本次请求的服务信息，会根据{@link #getUrl()}这个配置规则生成一个新的URL
     * @return
     */
    URL configure(URL url);

}
