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
package com.alibaba.dubbo.rpc.cluster.directory;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.cluster.RouterFactory;
import com.alibaba.dubbo.rpc.cluster.router.MockInvokersSelector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract implementation of Directory: Invoker list returned from this Directory's list method have been filtered by Routers
 *
 */
public abstract class AbstractDirectory<T> implements Directory<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDirectory.class);



    /** 用来表示提供服务目标机器，可以是多个 */
    private final URL url;
    /** Directory是用来封装同一服务接口的多个服务提供者，该字段用来标识该服务接口是否已被注销 */
    private volatile boolean destroyed = false;
    /** 表示本次服务调用的URL信息 */
    private volatile URL consumerUrl;
    /** 获取所有的服务提供后，会通过Router进行过滤后，再使用Directory封装多个服务提供者，最后通过负载均衡确定一个要用于本次调用的目标服务*/
    private volatile List<Router> routers;





    public AbstractDirectory(URL url) {
        this(url, null);
    }
    public AbstractDirectory(URL url, List<Router> routers) {
        this(url, url, routers);
    }
    public AbstractDirectory(URL url, URL consumerUrl, List<Router> routers) {
        if (url == null)
            throw new IllegalArgumentException("url == null");
        this.url = url;
        this.consumerUrl = consumerUrl;
        setRouters(routers);
    }




    // 核心方法

    /**
     * 根据请求的方法签名及入参，返回多个{@link Invoker}对象（可能存在多个服务提供者）。
     * {@link Cluster}会将 Directory 中的多个 Invoker 伪装成一个 Invoker, 对上层透明，包含集群的容错机制
     *
     * 通过Router进行过滤后，会使用Directory封装多个服务提供者，最后通过负载均衡确定一个要用于本次调用的目标服务
     *
     * @return invokers
     */
    public List<Invoker<T>> list(Invocation invocation) throws RpcException {
        if (destroyed) {
            throw new RpcException("Directory already destroyed .url: " + getUrl());
        }

        // 先获取所有的服务提供者，该实现由子类扩展
        List<Invoker<T>> invokers = doList(invocation);

        // 遍历所有的路由规则，对Invoker进行过滤
        List<Router> localRouters = this.routers;
        if (localRouters != null && localRouters.size() > 0) {
            for (Router router : localRouters) {
                try {
                    if (router.getUrl() == null || router.getUrl().getParameter(Constants.RUNTIME_KEY, false)) {
                        invokers = router.route(invokers, getConsumerUrl(), invocation);
                    }
                } catch (Throwable t) {
                    logger.error("Failed to execute router: " + getUrl() + ", cause: " + t.getMessage(), t);
                }
            }
        }
        return invokers;
    }
    protected abstract List<Invoker<T>> doList(Invocation invocation) throws RpcException;



    public void destroy() {
        destroyed = true;
    }



    // getter and setter ...
    public boolean isDestroyed() {
        return destroyed;
    }
    public URL getUrl() {
        return url;
    }
    public List<Router> getRouters() {
        return routers;
    }
    protected void setRouters(List<Router> routers) {
        // copy list
        routers = routers == null ? new ArrayList<Router>() : new ArrayList<Router>(routers);
        // append url router
        String routerkey = url.getParameter(Constants.ROUTER_KEY);
        if (routerkey != null && routerkey.length() > 0) {
            RouterFactory routerFactory = ExtensionLoader.getExtensionLoader(RouterFactory.class).getExtension(routerkey);
            routers.add(routerFactory.getRouter(url));
        }
        // append mock invoker selector
        routers.add(new MockInvokersSelector());
        Collections.sort(routers);
        this.routers = routers;
    }
    public URL getConsumerUrl() {
        return consumerUrl;
    }
    public void setConsumerUrl(URL consumerUrl) {
        this.consumerUrl = consumerUrl;
    }

}