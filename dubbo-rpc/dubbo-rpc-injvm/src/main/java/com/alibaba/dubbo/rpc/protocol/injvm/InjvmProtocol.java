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
package com.alibaba.dubbo.rpc.protocol.injvm;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProtocol;
import com.alibaba.dubbo.rpc.support.ProtocolUtils;

import java.util.Map;

/**
 * InjvmProtocol：本地服务导出协议，需要注意的是这一个static实例，所有一个JVM需要导出的服务都通过该实例导出，并最终缓存到InjvmExporter中
 */
public class InjvmProtocol extends AbstractProtocol implements Protocol {

    /** 本地服务的协议名称：injvm */
    public static final String NAME = Constants.LOCAL_PROTOCOL;

    public static final int DEFAULT_PORT = 0;

    private static InjvmProtocol INSTANCE;

    public InjvmProtocol() {
        INSTANCE = this;
    }
    public static InjvmProtocol getInjvmProtocol() {
        if (INSTANCE == null) {
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(InjvmProtocol.NAME); // load
        }
        return INSTANCE;
    }


    /**
     * 导出服务：创建一个InjvmExporter实例
     *
     * @param invoker           服务的执行体
     * @param <T>
     * @return
     * @throws RpcException
     */
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        return new InjvmExporter<T>(invoker, invoker.getUrl().getServiceKey(), exporterMap);
    }

    /**
     * 服务应用，创建一个InjvmInvoker实例
     *
     * @param serviceType
     * @param url               远程服务的URL地址
     * @param <T>
     * @return
     * @throws RpcException
     */
    public <T> Invoker<T> refer(Class<T> serviceType, URL url) throws RpcException {
        return new InjvmInvoker<T>(serviceType, url, url.getServiceKey(), exporterMap);
    }

    /**
     * 返回该url对应的Exporter
     *
     * @param map
     * @param key
     * @return
     */
    static Exporter<?> getExporter(Map<String, Exporter<?>> map, URL key) {
        Exporter<?> result = null;

        // 当服serviceKey包含*时，返回该serviceKey对应的Exporter
        if (!key.getServiceKey().contains("*")) {
            result = map.get(key.getServiceKey());
        } else {
            // 返回该url匹配的Exporter
            if (map != null && !map.isEmpty()) {
                for (Exporter<?> exporter : map.values()) {
                    // 判断ServiceKey是否一样，当且仅当interface、group、version都一样时，返回true
                    if (UrlUtils.isServiceKeyMatch(key, exporter.getInvoker().getUrl())) {
                        result = exporter;
                        break;
                    }
                }
            }
        }

        if (result == null) {
            return null;
        }
        // 如果该url是泛化调用，则返回null
        else if (ProtocolUtils.isGeneric(result.getInvoker().getUrl().getParameter(Constants.GENERIC_KEY))) {
            return null;
        } else {
            return result;
        }
    }

    public int getDefaultPort() {
        return DEFAULT_PORT;
    }


    /**
     * 判断url是否为本地调用，当url的scope=local，或者injvm=true，或者本地url有匹配的本地Exporter时，该方法返回true
     *
     * @param url
     * @return
     */
    public boolean isInjvmRefer(URL url) {
        final boolean isJvmRefer;
        String scope = url.getParameter(Constants.SCOPE_KEY);
        // 由于injvm协议是显式配置的，我们不需要设置任何额外的标志，使用普通的reference process。
        if (Constants.LOCAL_PROTOCOL.toString().equals(url.getProtocol())) {
            isJvmRefer = false;
        } else if (Constants.SCOPE_LOCAL.equals(scope) || (url.getParameter("injvm", false))) {
            // 如果它被声明为本地引用“scope=local”等价于“injvm=true”，在后续的版本中将不推荐使用injvm
            isJvmRefer = true;
        } else if (Constants.SCOPE_REMOTE.equals(scope)) {
            // 它被声明为远程引用
            isJvmRefer = false;
        } else if (url.getParameter(Constants.GENERIC_KEY, false)) {
            // 泛型调用不是本地引用
            isJvmRefer = false;
        } else if (getExporter(exporterMap, url) != null) {
            // 默认情况下，如果本地有公开的服务，通过本地引用
            isJvmRefer = true;
        } else {
            isJvmRefer = false;
        }
        return isJvmRefer;
    }
}