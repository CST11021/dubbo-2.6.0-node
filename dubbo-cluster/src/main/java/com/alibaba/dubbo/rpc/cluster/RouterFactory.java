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
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;
import com.alibaba.dubbo.rpc.Invocation;

/**
 *
 * 路由工厂，它有三个实现：
 * ①条件路由工厂：{@link com.alibaba.dubbo.rpc.cluster.router.condition.ConditionRouterFactory}
 * ②脚本路由工厂：{@link com.alibaba.dubbo.rpc.cluster.router.script.ScriptRouterFactory}
 * ③文件路由工厂：{@link com.alibaba.dubbo.rpc.cluster.router.file.FileRouterFactory}
 *
 * 文件路由工厂其实是将用来创建条件路由或脚本路由的，只是该工厂是通过加载本地文件配置，来创建路由
 * 条件路由和脚本路由都需要配置一些路由规则，我们可以将这些配置保存在文件中，然后通过文件路由工厂来加载，最终创建的路由规则还是条件或脚本路由
 *
 *
 * RouterFactory. (SPI, Singleton, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Routing">Routing</a>
 *
 * @see com.alibaba.dubbo.rpc.cluster.Cluster#join(Directory)
 * @see com.alibaba.dubbo.rpc.cluster.Directory#list(Invocation)
 */
@SPI
public interface RouterFactory {

    /**
     * Create router.
     *
     * @param url
     * @return router
     */
    @Adaptive("protocol")
    Router getRouter(URL url);

}