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

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * Protocol. (API/SPI, Singleton, ThreadSafe)
 * 例如使用dubbo协议，
 *
 * 服务导出时，它的一个执行顺序是：
 * ① 先将服务暴露到本地：ProtocolFilterWrapper -> ProtocolListenerWrapper -> InjvmProtocol
 * ② 将服务暴露到注册中心：ProtocolFilterWrapper -> ProtocolListenerWrapper -> RegistryProtocol
 * ③ 根据协议创建服务监听，等待消费者调用：ProtocolFilterWrapper -> ProtocolListenerWrapper -> DubboProtocol
 *
 * 注意：RegistryProtocol内部存在存在一个指向DubboProtocol的引用，程序执行时会先将服务暴露出来等待调用，然后在暴露到注册中心
 *
 * 服务导入时，它的一个执行顺序是：
 * ① ProtocolListenerWrapper -> ProtocolFilterWrapper -> RegistryProtocol
 * ① ProtocolListenerWrapper -> ProtocolFilterWrapper -> DubboProtocol
 *
 *
 */
@SPI("dubbo")
public interface Protocol {

    /**
     * 获取获取协议的默认端口，不同的协议有不同的默认端口，比如dubbo协议的默认是20880，HTTP协议的默认端口是80
     *
     * @return default port
     */
    int getDefaultPort();

    /**
     * 导出服务以进行远程调用：
     * 1. 协议应在收到请求后记录请求源地址：RpcContext.getContext().setRemoteAddress();
     * 2. export()必须是幂等的，即，导出相同的URL时一次调用和两次调用之间没有区别
     * 3.调用程序实例由框架传递，协议无需关心
     *
     * @param <T>               服务的类型
     * @param invoker           服务的执行体
     * @return exporter         暴露服务的引用，用于取消暴露
     * @throws RpcException     当暴露服务出错时抛出，比如端口已占用
     *
     *
     *  Dubbo处理服务暴露的关键就在Invoker转换到Exporter的过程，我们以Dubbo和rmi这两种典型协议的实现来进行说明：
     *      Dubbo的实现：
     *          Dubbo协议的Invoker转为Exporter发生在DubboProtocol类的export方法，它主要是打开socket侦听服务，并接收客户端发来的各种请求，通讯细节由dubbo自己实现。
     *
     *      Rmi的实现：
     *          RMI协议的Invoker转为Exporter发生在RmiProtocol类的export方法，他通过Spring或Dubbo或JDK来实现服务，通讯细节由JDK底层来实现。
     *
     */
    @Adaptive
    <T> Exporter<T> export(Invoker<T> invoker) throws RpcException;

    /**
     * Refer a remote service: <br>
     * 1. 当用户调用从refer()调用返回的Invoker对象的invoke()方法时，协议需要相应地执行Invoker对象的invoke()方法。
     * 2. 实现由`refer()`返回的`Invoker`是协议的责任。一般来说，协议在Invoker实现中发送远程请求。
     * 3. 如果在URL中设置了check = false，则实现不得抛出异常，而应尝试在连接失败时恢复。
     *
     * @param <T>               服务的类型
     * @param type              服务的类型
     * @param url               远程服务的URL地址
     * @return invoker          服务的本地代理
     * @throws RpcException     当连接服务提供方失败时抛出
     */
    @Adaptive
    <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException;

    /**
     * Destroy protocol: <br>
     * 1. Cancel all services this protocol exports and refers <br>
     * 2. Release all occupied resources, for example: connection, port, etc. <br>
     * 3. Protocol can continue to export and refer new service even after it's destroyed.
     */
    void destroy();

}