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
package com.alibaba.dubbo.rpc.protocol;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.List;

/**
 * 使用装饰器模式来包装Protocol对象，所有的协议实现都会用该包装器来包装：该包装器优先于{@link ProtocolListenerWrapper}执行
 *
 * 该包装类的作用是：在对服务进行导出或引入时，对Invoker进行包装，并添加过滤器
 */
public class ProtocolFilterWrapper implements Protocol {

    /**
     * 通过SPI自适应扩展机制:
     * 服务导出时：该实现类是{@link ProtocolListenerWrapper}
     * 服务导入时：该实现类对应真正的协议实现，例如：RegistryProtocol、dubbo协议，HTTP协议、injvm协议等
     */
    private final Protocol protocol;

    public ProtocolFilterWrapper(Protocol protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null");
        }
        this.protocol = protocol;
    }



    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        // 如果协议是"registry"说明，该请求是要将服务暴露到注册中心的
        if (Constants.REGISTRY_PROTOCOL.equals(invoker.getUrl().getProtocol())) {
            // 如果该URL是要暴露到注册中心的，则直接调用对应的协议扩展点实现来暴露服务，这里的扩展点是RegistryProtocol
            return protocol.export(invoker);
        }

        // 服务暴露前会调用一系列的过滤器
        return protocol.export(buildInvokerChain(invoker, Constants.SERVICE_FILTER_KEY, Constants.PROVIDER));
    }
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
            return protocol.refer(type, url);
        }
        return buildInvokerChain(protocol.refer(type, url), Constants.REFERENCE_FILTER_KEY, Constants.CONSUMER);
    }

    /**
     * 在具体协议导出和导入服务前都会调用该方法，
     *
     * @param invoker
     * @param key
     * @param group
     * @param <T>
     * @return
     */
    private static <T> Invoker<T> buildInvokerChain(final Invoker<T> invoker, String key, String group) {
        Invoker<T> last = invoker;
        List<Filter> filters = ExtensionLoader.getExtensionLoader(Filter.class).getActivateExtension(invoker.getUrl(), key, group);

        // 服务导出，过滤链如：EchoFilter -> ClassLoaderFilter -> GenericFilter -> ContextFilter -> TraceFilter -> TimeoutFilter -> MonitorFilter -> ExceptionFilter
        // 服务引入，过滤链如：ConsumerContextFilter -> FutureFilter -> MonitorFilter
        if (filters.size() > 0) {
            for (int i = filters.size() - 1; i >= 0; i--) {
                final Filter filter = filters.get(i);
                final Invoker<T> next = last;
                last = new Invoker<T>() {

                    public Class<T> getInterface() {
                        return invoker.getInterface();
                    }

                    public URL getUrl() {
                        return invoker.getUrl();
                    }

                    public boolean isAvailable() {
                        return invoker.isAvailable();
                    }

                    public Result invoke(Invocation invocation) throws RpcException {
                        return filter.invoke(next, invocation);
                    }

                    public void destroy() {
                        invoker.destroy();
                    }

                    @Override
                    public String toString() {
                        return invoker.toString();
                    }
                };
            }
        }
        return last;
    }




    // 没有其他逻辑

    public int getDefaultPort() {
        return protocol.getDefaultPort();
    }
    public void destroy() {
        protocol.destroy();
    }

}