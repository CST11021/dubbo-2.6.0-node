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
package com.alibaba.dubbo.remoting.exchange;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.support.ExchangeHandlerDispatcher;
import com.alibaba.dubbo.remoting.exchange.support.Replier;
import com.alibaba.dubbo.remoting.transport.ChannelHandlerAdapter;

/**
 * Exchanger facade. (API, Static, ThreadSafe)
 */
public class Exchangers {

    static {
        // check duplicate jar package
        Version.checkDuplicate(Exchangers.class);
    }

    private Exchangers() {
    }

    // -------------------------------------------------
    // 服务端调用bind方法，创建一个服务监听，等待客户端连接，注意：bind方法一定要有Replier，而connect不一定需要Replier
    // -------------------------------------------------


    /**
     * 重要的方法：DubboProtocol就是通过调用该方法，来创建一个ExchangeServer实例，使dubbo服务提供者，具有接收服务请求的能力，同时服务提供者
     * 通过扩展ExchangeHandler#reply方法，来处理接收到的请求，将请求交由具体的业务实现，然后将处理结果反馈给服务消费者。
     *
     * @param url
     * @param handler
     * @return
     * @throws RemotingException
     */
    public static ExchangeServer bind(URL url, ExchangeHandler handler) throws RemotingException {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        url = url.addParameterIfAbsent(Constants.CODEC_KEY, "exchange");
        return getExchanger(url).bind(url, handler);
    }
    public static ExchangeServer bind(String url, ExchangeHandler handler) throws RemotingException {
        return bind(URL.valueOf(url), handler);
    }
    /**
     * 该方法创建的ExchangeServer，具有请求分发能力
     *
     * @param url
     * @param handler
     * @param replier
     * @return
     * @throws RemotingException
     */
    public static ExchangeServer bind(URL url, ChannelHandler handler, Replier<?> replier) throws RemotingException {
        // 注意：这里使用的是ExchangeHandlerDispatcher实现，该实现具有请求分发能力
        return bind(url, new ExchangeHandlerDispatcher(replier, handler));
    }
    public static ExchangeServer bind(String url, ChannelHandler handler, Replier<?> replier) throws RemotingException {
        return bind(URL.valueOf(url), handler, replier);
    }
    public static ExchangeServer bind(URL url, Replier<?> replier) throws RemotingException {
        return bind(url, new ChannelHandlerAdapter(), replier);
    }
    public static ExchangeServer bind(String url, Replier<?> replier) throws RemotingException {
        return bind(URL.valueOf(url), replier);
    }








    // -------------------------------------------------
    // 客户端调用connect方法，建立Socket连接
    // -------------------------------------------------

    public static ExchangeClient connect(String url) throws RemotingException {
        return connect(URL.valueOf(url));
    }
    public static ExchangeClient connect(URL url) throws RemotingException {
        return connect(url, new ChannelHandlerAdapter(), null);
    }
    public static ExchangeClient connect(String url, Replier<?> replier) throws RemotingException {
        return connect(URL.valueOf(url), new ChannelHandlerAdapter(), replier);
    }
    public static ExchangeClient connect(URL url, Replier<?> replier) throws RemotingException {
        return connect(url, new ChannelHandlerAdapter(), replier);
    }
    /**
     *
     *
     * @param url
     * @param handler   通道事件监听
     * @param replier   消息处理器：当从通道接收到消息时，通过replier对消息作出处理
     * @return
     * @throws RemotingException
     */
    public static ExchangeClient connect(String url, ChannelHandler handler, Replier<?> replier) throws RemotingException {
        return connect(URL.valueOf(url), handler, replier);
    }
    public static ExchangeClient connect(URL url, ChannelHandler handler, Replier<?> replier) throws RemotingException {
        return connect(url, new ExchangeHandlerDispatcher(replier, handler));
    }
    public static ExchangeClient connect(String url, ExchangeHandler handler) throws RemotingException {
        return connect(URL.valueOf(url), handler);
    }
    public static ExchangeClient connect(URL url, ExchangeHandler handler) throws RemotingException {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        url = url.addParameterIfAbsent(Constants.CODEC_KEY, "exchange");
        return getExchanger(url).connect(url, handler);
    }




    // -------------------------------------------------
    // 基于传输层之上，实现 Request-Response 信息交换语义
    // -------------------------------------------------

    public static Exchanger getExchanger(String type) {
        return ExtensionLoader.getExtensionLoader(Exchanger.class).getExtension(type);
    }
    public static Exchanger getExchanger(URL url) {
        String type = url.getParameter(Constants.EXCHANGER_KEY, Constants.DEFAULT_EXCHANGER);
        return getExchanger(type);
    }


}