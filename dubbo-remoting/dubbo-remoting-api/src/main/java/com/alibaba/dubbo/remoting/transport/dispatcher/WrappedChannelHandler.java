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
package com.alibaba.dubbo.remoting.transport.dispatcher;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.store.DataStore;
import com.alibaba.dubbo.common.threadpool.ThreadPool;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.ChannelHandlerDelegate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用于包装ChannelHandler，将通道事件处理移交给线程池处理
 */
public class WrappedChannelHandler implements ChannelHandlerDelegate {

    protected static final Logger logger = LoggerFactory.getLogger(WrappedChannelHandler.class);

    protected static final ExecutorService SHARED_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("DubboSharedHandler", true));

    protected final ExecutorService executor;

    protected final ChannelHandler handler;

    protected final URL url;

    public WrappedChannelHandler(ChannelHandler handler, URL url) {
        this.handler = handler;
        this.url = url;
        executor = (ExecutorService) ExtensionLoader.getExtensionLoader(ThreadPool.class).getAdaptiveExtension().getExecutor(url);

        String componentKey = Constants.EXECUTOR_SERVICE_COMPONENT_KEY;
        if (Constants.CONSUMER_SIDE.equalsIgnoreCase(url.getParameter(Constants.SIDE_KEY))) {
            componentKey = Constants.CONSUMER_SIDE;
        }
        DataStore dataStore = ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension();
        dataStore.put(componentKey, Integer.toString(url.getPort()), executor);
    }

    public void close() {
        try {
            if (executor instanceof ExecutorService) {
                ((ExecutorService) executor).shutdown();
            }
        } catch (Throwable t) {
            logger.warn("fail to destroy thread pool of server: " + t.getMessage(), t);
        }
    }

    /**
     * 当客户端与服务端建立通道连接时，调用该方法
     *
     * @param channel channel.
     */
    public void connected(Channel channel) throws RemotingException {
        handler.connected(channel);
    }

    /**
     * 当客户端与服务端的通道连接断开时，调用该方法
     *
     * @param channel channel.
     */
    public void disconnected(Channel channel) throws RemotingException {
        handler.disconnected(channel);
    }

    /**
     * 向客户端（消费者）发送一个消息时，调用该方法
     *
     * @param channel 用于发送消息的通道
     * @param message 要发送的消息
     */
    public void sent(Channel channel, Object message) throws RemotingException {
        handler.sent(channel, message);
    }

    /**
     * 当接收到客户端请求的调用该方法
     *
     * @param channel 用于接收消息的通道.
     * @param message 要接收的消息.
     */
    public void received(Channel channel, Object message) throws RemotingException {
        handler.received(channel, message);
    }

    /**
     * 通信异常时调用该方法
     *
     * @param channel   channel.
     * @param exception exception.
     */
    public void caught(Channel channel, Throwable exception) throws RemotingException {
        handler.caught(channel, exception);
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * 获取被代理的ChannelHandler
     *
     * @return
     */
    public ChannelHandler getHandler() {
        if (handler instanceof ChannelHandlerDelegate) {
            return ((ChannelHandlerDelegate) handler).getHandler();
        } else {
            return handler;
        }
    }

    public URL getUrl() {
        return url;
    }

}