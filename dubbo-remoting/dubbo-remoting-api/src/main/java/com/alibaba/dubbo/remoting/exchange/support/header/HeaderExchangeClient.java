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
package com.alibaba.dubbo.remoting.exchange.support.header;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * HeaderExchangeClient：dubbo中默认的ExchangeClient实现
 * 1、Request/Response内部实现委托给了ExchangeChannel；
 * 2、HeaderExchangeClient在ExchangeChannel基础上增加了心跳检测机制；
 */
public class HeaderExchangeClient implements ExchangeClient {

    private static final Logger logger = LoggerFactory.getLogger(HeaderExchangeClient.class);

    private static final ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(2, new NamedThreadFactory("dubbo-remoting-client-heartbeat", true));

    /** 客户端对象 */
    private final Client client;
    /** 用于通信的通道，ExchangeChannel主要用于实现request/response语义，这里在构造器中被实例化为HeaderExchangeChannel对象 */
    private final ExchangeChannel channel;

    /** 心跳计时器 */
    private ScheduledFuture<?> heartbeatTimer;
    private int heartbeat;
    /** 心跳超时时间（毫秒），默认值为0，将不执行心跳 */
    private int heartbeatTimeout;

    public HeaderExchangeClient(Client client, boolean needHeartbeat) {
        if (client == null) {
            throw new IllegalArgumentException("client == null");
        }

        this.client = client;
        this.channel = new HeaderExchangeChannel(client);
        String dubbo = client.getUrl().getParameter(Constants.DUBBO_VERSION_KEY);
        this.heartbeat = client.getUrl().getParameter(Constants.HEARTBEAT_KEY, dubbo != null && dubbo.startsWith("1.0.") ? Constants.DEFAULT_HEARTBEAT : 0);
        this.heartbeatTimeout = client.getUrl().getParameter(Constants.HEARTBEAT_TIMEOUT_KEY, heartbeat * 3);
        if (heartbeatTimeout < heartbeat * 2) {
            throw new IllegalStateException("heartbeatTimeout < heartbeatInterval * 2");
        }
        if (needHeartbeat) {
            startHeatbeatTimer();
        }
    }

    /**
     * 向服务端发起请求（即向ExchangeChannel发送请求，并从通道获取一个Response对象）
     *
     * @param request
     * @return 服务端返回的响应数据
     * @throws RemotingException
     */
    public ResponseFuture request(Object request) throws RemotingException {
        return channel.request(request);
    }
    public ResponseFuture request(Object request, int timeout) throws RemotingException {
        return channel.request(request, timeout);
    }

    public URL getUrl() {
        return channel.getUrl();
    }

    /**
     * 获取服务端的地址
     *
     * @return
     */
    public InetSocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }

    /**
     * 获取通道事件监听处理器
     *
     * @return
     */
    public ChannelHandler getChannelHandler() {
        return channel.getChannelHandler();
    }

    /**
     * 判断该通道是否处于连接状态（即客户端和服务端是否处于连接状态）
     *
     * @return connected
     */
    public boolean isConnected() {
        return channel.isConnected();
    }

    /**
     * 获取客户端的本地地址
     *
     * @return
     */
    public InetSocketAddress getLocalAddress() {
        return channel.getLocalAddress();
    }

    public ExchangeHandler getExchangeHandler() {
        return channel.getExchangeHandler();
    }

    /**
     * 向通道发送信息，即向服务端发送请求，这里是单向通信
     *
     * @param message
     * @throws RemotingException
     */
    public void send(Object message) throws RemotingException {
        channel.send(message);
    }
    public void send(Object message, boolean sent) throws RemotingException {
        channel.send(message, sent);
    }

    public boolean isClosed() {
        return channel.isClosed();
    }

    public void close() {
        doClose();
        channel.close();
    }

    public void close(int timeout) {
        // Mark the client into the closure process
        startClose();
        doClose();
        channel.close(timeout);
    }

    @Override
    public void startClose() {
        channel.startClose();
    }

    public void reset(URL url) {
        client.reset(url);
    }

    @Deprecated
    public void reset(com.alibaba.dubbo.common.Parameters parameters) {
        reset(getUrl().addParameters(parameters.getParameters()));
    }

    public void reconnect() throws RemotingException {
        client.reconnect();
    }

    public Object getAttribute(String key) {
        return channel.getAttribute(key);
    }
    public void setAttribute(String key, Object value) {
        channel.setAttribute(key, value);
    }
    public void removeAttribute(String key) {
        channel.removeAttribute(key);
    }
    public boolean hasAttribute(String key) {
        return channel.hasAttribute(key);
    }

    /**
     * 启动客户端的心跳检测定时任务
     */
    private void startHeatbeatTimer() {
        stopHeartbeatTimer();
        if (heartbeat > 0) {
            heartbeatTimer = scheduled.scheduleWithFixedDelay(
                    new HeartBeatTask(new HeartBeatTask.ChannelProvider() {
                        public Collection<Channel> getChannels() {
                            return Collections.<Channel>singletonList(HeaderExchangeClient.this);
                        }
                    }, heartbeat, heartbeatTimeout),
                    heartbeat, heartbeat, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 停止心跳检测定时任务
     */
    private void stopHeartbeatTimer() {
        if (heartbeatTimer != null && !heartbeatTimer.isCancelled()) {
            try {
                heartbeatTimer.cancel(true);
                scheduled.purge();
            } catch (Throwable e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
        heartbeatTimer = null;
    }

    /**
     * 关闭客户端
     */
    private void doClose() {
        stopHeartbeatTimer();
    }

    @Override
    public String toString() {
        return "HeaderExchangeClient [channel=" + channel + "]";
    }
}