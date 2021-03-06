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
package com.alibaba.dubbo.remoting;

import com.alibaba.dubbo.common.URL;

import java.net.InetSocketAddress;

/**
 * Endpoint用于抽象通信节点（机器节点），Client和Server都继承于Endpoint
 *
 * Endpoint. (API/SPI, Prototype, ThreadSafe)
 *
 *
 * @see com.alibaba.dubbo.remoting.Channel
 * @see com.alibaba.dubbo.remoting.Client
 * @see com.alibaba.dubbo.remoting.Server
 */
public interface Endpoint {

    /** 用于描述目标机器，比如：IP/端口 */
    URL getUrl();

    /** 获取通道事件监听处理器 */
    ChannelHandler getChannelHandler();

    /** 表示当该节点的地址 */
    InetSocketAddress getLocalAddress();

    /**
     * 向通道发送一个消息，可能是客户端向服务端发起请求，也可能是服务端响应客户端请求
     *
     * @param message
     * @throws RemotingException
     */
    void send(Object message) throws RemotingException;
    /**
     * 向所有的通道发送消息
     *
     * @param message
     * @param sent    是否阻塞直到等待异步操作结束
     * @throws RemotingException
     */
    void send(Object message, boolean sent) throws RemotingException;

    /** 关闭通道 */
    void close();
    /** 优雅的关闭通道 */
    void close(int timeout);
    /** 开始关闭通道 */
    void startClose();
    /** 判断是否已经关闭通道*/
    boolean isClosed();

}