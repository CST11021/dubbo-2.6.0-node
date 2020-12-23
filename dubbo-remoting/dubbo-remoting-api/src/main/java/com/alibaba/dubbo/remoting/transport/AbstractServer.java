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
package com.alibaba.dubbo.remoting.transport;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.store.DataStore;
import com.alibaba.dubbo.common.utils.ExecutorUtil;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Server;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * AbstractServer
 */
public abstract class AbstractServer extends AbstractEndpoint implements Server {

    private static final Logger logger = LoggerFactory.getLogger(AbstractServer.class);

    protected static final String SERVER_THREAD_POOL_NAME = "DubboServerHandler";

    /** 用于处理客户端请求的线程池 */
    ExecutorService executor;
    /** 监听客户端请求的服务端地址（IP/端口） */
    private InetSocketAddress localAddress;
    /** 服务启动绑定的地址（一般和localAddress一样） */
    private InetSocketAddress bindAddress;


    // accepts和idleTimeout这两个属性目前看没啥用

    private int accepts;
    /** 600 seconds */
    private int idleTimeout = 600;

    /**
     * 初始化服务
     *
     * @param url
     * @param handler
     * @throws RemotingException
     */
    public AbstractServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
        localAddress = getUrl().toInetSocketAddress();

        // 初始化bindAddress
        String bindIp = getUrl().getParameter(Constants.BIND_IP_KEY, getUrl().getHost());
        int bindPort = getUrl().getParameter(Constants.BIND_PORT_KEY, getUrl().getPort());
        if (url.getParameter(Constants.ANYHOST_KEY, false) || NetUtils.isInvalidLocalHost(bindIp)) {
            bindIp = NetUtils.ANYHOST;
        }
        bindAddress = new InetSocketAddress(bindIp, bindPort);

        this.accepts = url.getParameter(Constants.ACCEPTS_KEY, Constants.DEFAULT_ACCEPTS);
        this.idleTimeout = url.getParameter(Constants.IDLE_TIMEOUT_KEY, Constants.DEFAULT_IDLE_TIMEOUT);

        // 启动一个服务，开始监听客户端请求
        try {
            doOpen();
            if (logger.isInfoEnabled()) {
                logger.info("Start " + getClass().getSimpleName() + " bind " + getBindAddress() + ", export " + getLocalAddress());
            }
        } catch (Throwable t) {
            throw new RemotingException(url.toInetSocketAddress(), null, "Failed to bind " + getClass().getSimpleName() + " on " + getLocalAddress() + ", cause: " + t.getMessage(), t);
        }

        DataStore dataStore = ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension();
        executor = (ExecutorService) dataStore.get(Constants.EXECUTOR_SERVICE_COMPONENT_KEY, Integer.toString(url.getPort()));
    }

    /**
     * 启动一个服务，开始监听客户端请求，实现逻辑交给各个通信框架自己实现
     *
     * @throws Throwable
     */
    protected abstract void doOpen() throws Throwable;

    /**
     * 关闭服务，实现逻辑交给各个通信框架自己实现
     *
     * @throws Throwable
     */
    protected abstract void doClose() throws Throwable;

    /**
     * 主要是从url获取配置的线程池数量，并设置dubbo通信的业务线程池
     *
     * @param url
     */
    public void reset(URL url) {
        if (url == null) {
            return;
        }

        try {
            if (url.hasParameter(Constants.ACCEPTS_KEY)) {
                int a = url.getParameter(Constants.ACCEPTS_KEY, 0);
                if (a > 0) {
                    this.accepts = a;
                }
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }

        try {
            if (url.hasParameter(Constants.IDLE_TIMEOUT_KEY)) {
                int t = url.getParameter(Constants.IDLE_TIMEOUT_KEY, 0);
                if (t > 0) {
                    this.idleTimeout = t;
                }
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }

        // 设置线程池大小
        try {
            if (url.hasParameter(Constants.THREADS_KEY) && executor instanceof ThreadPoolExecutor && !executor.isShutdown()) {
                ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
                int threads = url.getParameter(Constants.THREADS_KEY, 0);
                int max = threadPoolExecutor.getMaximumPoolSize();
                int core = threadPoolExecutor.getCorePoolSize();
                if (threads > 0 && (threads != max || threads != core)) {

                    if (threads < core) {
                        threadPoolExecutor.setCorePoolSize(threads);
                        if (core == max) {
                            threadPoolExecutor.setMaximumPoolSize(threads);
                        }
                    } else {
                        threadPoolExecutor.setMaximumPoolSize(threads);
                        if (core == max) {
                            threadPoolExecutor.setCorePoolSize(threads);
                        }
                    }

                }
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        super.setUrl(getUrl().addParameters(url.getParameters()));
    }

    /**
     * 向连接该服务的所有通道发送消息
     *
     * @param message
     * @param sent    already sent to socket?
     * @throws RemotingException
     */
    public void send(Object message, boolean sent) throws RemotingException {
        Collection<Channel> channels = getChannels();
        for (Channel channel : channels) {
            if (channel.isConnected()) {
                channel.send(message, sent);
            }
        }
    }

    public void close() {
        if (logger.isInfoEnabled()) {
            logger.info("Close " + getClass().getSimpleName() + " bind " + getBindAddress() + ", export " + getLocalAddress());
        }
        ExecutorUtil.shutdownNow(executor, 100);
        try {
            super.close();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            doClose();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public void close(int timeout) {
        ExecutorUtil.gracefulShutdown(executor, timeout);
        close();
    }




    /**
     * 获取服务端的地址
     *
     * @return
     */
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }
    public InetSocketAddress getBindAddress() {
        return bindAddress;
    }
    public int getAccepts() {
        return accepts;
    }
    public int getIdleTimeout() {
        return idleTimeout;
    }

    /**
     * 当客户端与服务端建立通道连接时，调用该方法
     *
     * @param ch channel.
     */
    @Override
    public void connected(Channel ch) throws RemotingException {
        // If the server has entered the shutdown process, reject any new connection
        if (this.isClosing() || this.isClosed()) {
            logger.warn("Close new channel " + ch + ", cause: server is closing or has been closed. For example, receive a new connect request while in shutdown process.");
            ch.close();
            return;
        }

        Collection<Channel> channels = getChannels();
        if (accepts > 0 && channels.size() > accepts) {
            logger.error("Close channel " + ch + ", cause: The server " + ch.getLocalAddress() + " connections greater than max config " + accepts);
            ch.close();
            return;
        }
        super.connected(ch);
    }

    /**
     * 当客户端与服务端的通道连接断开时，调用该方法
     *
     * @param ch channel.
     */
    @Override
    public void disconnected(Channel ch) throws RemotingException {
        Collection<Channel> channels = getChannels();
        if (channels.size() == 0) {
            logger.warn("All clients has discontected from " + ch.getLocalAddress() + ". You can graceful shutdown now.");
        }
        super.disconnected(ch);
    }

}