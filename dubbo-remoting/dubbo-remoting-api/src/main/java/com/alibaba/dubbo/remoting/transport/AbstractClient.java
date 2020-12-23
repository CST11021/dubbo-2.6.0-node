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
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.store.DataStore;
import com.alibaba.dubbo.common.utils.ExecutorUtil;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.dispatcher.ChannelHandlers;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AbstractPeer：用于描述通道对应的ChannelHandler和通道连接状态
 * AbstractEndpoint 扩展自AbstractPeer，添加了编解码功能和超时信息
 *
 * AbstractClient：用于子类扩展，不同的通讯框架有不同的实现，实现类如下：GrizzlyClient、MinaClient、NettyClient、NettyClient（4）
 *
 */
public abstract class AbstractClient extends AbstractEndpoint implements Client {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClient.class);

    protected static final String CLIENT_THREAD_POOL_NAME = "DubboClientHandler";
    private static final AtomicInteger CLIENT_THREAD_POOL_ID = new AtomicInteger();

    /** 重试连接时用的线程池，总不能每次重试连接再去创建一个新的线程吧 */
    private static final ScheduledThreadPoolExecutor reconnectExecutorService = new ScheduledThreadPoolExecutor(2, new NamedThreadFactory("DubboClientReconnectTimer", true));
    /** 表示在ScheduledExecutorService中提交了任务的返回结果，我们通过Delayed的接口getDelay()方法知道该任务还有多久才被执行 */
    private volatile ScheduledFuture<?> reconnectExecutorFuture = null;

    /** 客户端与服务端建立连接时用的锁 */
    private final Lock connectLock = new ReentrantLock();
    /** 表示客户端向服务端发起请求时，是否要重新建立连接 */
    private final boolean send_reconnect;
    /** 表示重置连接的次数 */
    private final AtomicInteger reconnect_count = new AtomicInteger(0);
    /** 表示连接使出现异常的日志信息是否已经打印了，如果没有该标记则每次重置连接失败后都会打印日志 */
    private final AtomicBoolean reconnect_error_log_flag = new AtomicBoolean(false);
    /** 每n次重置与服务端建立连接都会打印日志，比如该值为2，则表示没两次连接失败打印一次日志 */
    private final int reconnect_warning_period;
    private final long shutdown_timeout;
    protected volatile ExecutorService executor;
    /** 表示最后一次成功连接的时间 */
    private long lastConnectedTime = System.currentTimeMillis();


    public AbstractClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);

        send_reconnect = url.getParameter(Constants.SEND_RECONNECT_KEY, false);
        shutdown_timeout = url.getParameter(Constants.SHUTDOWN_TIMEOUT_KEY, Constants.DEFAULT_SHUTDOWN_TIMEOUT);
        // The default reconnection interval is 2s, 1800 means warning interval is 1 hour.
        reconnect_warning_period = url.getParameter("reconnect.waring.period", 1800);

        // 打开（创建）一个客户端
        try {
            doOpen();
        } catch (Throwable t) {
            close();
            throw new RemotingException(url.toInetSocketAddress(), null,
                    "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                            + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(), t);
        }

        // 连接到服务端
        try {
            connect();
            if (logger.isInfoEnabled()) {
                logger.info("Start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress() + " connect to the server " + getRemoteAddress());
            }
        } catch (RemotingException t) {
            if (url.getParameter(Constants.CHECK_KEY, true)) {
                close();
                throw t;
            } else {
                logger.warn("Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                        + " connect to the server " + getRemoteAddress() + " (check == false, ignore and retry later!), cause: " + t.getMessage(), t);
            }
        } catch (Throwable t) {
            close();
            throw new RemotingException(url.toInetSocketAddress(), null,
                    "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                            + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(), t);
        }

        executor = (ExecutorService) ExtensionLoader.getExtensionLoader(DataStore.class)
                .getDefaultExtension().get(Constants.CONSUMER_SIDE, Integer.toString(url.getPort()));
        ExtensionLoader.getExtensionLoader(DataStore.class)
                .getDefaultExtension().remove(Constants.CONSUMER_SIDE, Integer.toString(url.getPort()));
    }

    protected static ChannelHandler wrapChannelHandler(URL url, ChannelHandler handler) {
        url = ExecutorUtil.setThreadName(url, CLIENT_THREAD_POOL_NAME);
        url = url.addParameterIfAbsent(Constants.THREADPOOL_KEY, Constants.DEFAULT_CLIENT_THREADPOOL);
        return ChannelHandlers.wrap(handler, url);
    }

    protected ExecutorService createExecutor() {
        return Executors.newCachedThreadPool(new NamedThreadFactory(CLIENT_THREAD_POOL_NAME + CLIENT_THREAD_POOL_ID.incrementAndGet() + "-" + getUrl().getAddress(), true));
    }

    public InetSocketAddress getConnectAddress() {
        return new InetSocketAddress(NetUtils.filterLocalHost(getUrl().getHost()), getUrl().getPort());
    }

    public InetSocketAddress getRemoteAddress() {
        Channel channel = getChannel();
        if (channel == null)
            return getUrl().toInetSocketAddress();
        return channel.getRemoteAddress();
    }

    public InetSocketAddress getLocalAddress() {
        Channel channel = getChannel();
        if (channel == null)
            return InetSocketAddress.createUnresolved(NetUtils.getLocalHost(), 0);
        return channel.getLocalAddress();
    }



    public Object getAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null)
            return null;
        return channel.getAttribute(key);
    }
    public void setAttribute(String key, Object value) {
        Channel channel = getChannel();
        if (channel == null)
            return;
        channel.setAttribute(key, value);
    }
    public void removeAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null)
            return;
        channel.removeAttribute(key);
    }
    public boolean hasAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null)
            return false;
        return channel.hasAttribute(key);
    }

    /**
     * 将发送消息的操作委托给Channel去实现
     *
     * @param message
     * @param sent    already sent to socket?
     * @throws RemotingException
     */
    public void send(Object message, boolean sent) throws RemotingException {
        if (send_reconnect && !isConnected()) {
            connect();
        }
        Channel channel = getChannel();
        //TODO Can the value returned by getChannel() be null? need improvement.
        if (channel == null || !channel.isConnected()) {
            throw new RemotingException(this, "message can not send, because channel is closed . url:" + getUrl());
        }
        channel.send(message, sent);
    }

    /** 与服务端建立连接 */
    protected void connect() throws RemotingException {
        connectLock.lock();
        try {
            if (isConnected()) {
                return;
            }
            // 初始化重置连接的线程
            initConnectStatusCheckCommand();
            doConnect();
            if (!isConnected()) {
                throw new RemotingException(this, "Failed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                        + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                        + ", cause: Connect wait timeout: " + getTimeout() + "ms.");
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("Successed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                            + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                            + ", channel is " + this.getChannel());
                }
            }
            reconnect_count.set(0);
            reconnect_error_log_flag.set(false);
        } catch (RemotingException e) {
            throw e;
        } catch (Throwable e) {
            throw new RemotingException(this, "Failed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                    + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                    + ", cause: " + e.getMessage(), e);
        } finally {
            connectLock.unlock();
        }
    }
    /** 判断是否已经与服务端建立连接 */
    public boolean isConnected() {
        Channel channel = getChannel();
        if (channel == null)
            return false;
        return channel.isConnected();
    }
    /** 初始化重置连接的线程 */
    private synchronized void initConnectStatusCheckCommand() {
        // 获取重置连接的时间间隔
        int reconnect = getReconnectParam(getUrl());
        if (reconnect > 0 && (reconnectExecutorFuture == null || reconnectExecutorFuture.isCancelled())) {
            Runnable connectStatusCheckCommand = new Runnable() {
                public void run() {
                    try {
                        if (!isConnected()) {
                            connect();
                        } else {
                            lastConnectedTime = System.currentTimeMillis();
                        }
                    } catch (Throwable t) {
                        String errorMsg = "client reconnect to " + getUrl().getAddress() + " find error . url: " + getUrl();
                        // wait registry sync provider list
                        if (System.currentTimeMillis() - lastConnectedTime > shutdown_timeout) {
                            if (!reconnect_error_log_flag.get()) {
                                reconnect_error_log_flag.set(true);
                                logger.error(errorMsg, t);
                                return;
                            }
                        }
                        if (reconnect_count.getAndIncrement() % reconnect_warning_period == 0) {
                            logger.warn(errorMsg, t);
                        }
                    }
                }
            };
            reconnectExecutorFuture = reconnectExecutorService.scheduleWithFixedDelay(connectStatusCheckCommand, reconnect, reconnect, TimeUnit.MILLISECONDS);
        }
    }
    /**
     * 获取本次连接请求的重置连接时间间隔
     * @param url
     * @return 0-false
     */
    private static int getReconnectParam(URL url) {
        int reconnect;
        String param = url.getParameter(Constants.RECONNECT_KEY);
        if (param == null || param.length() == 0 || "true".equalsIgnoreCase(param)) {
            reconnect = Constants.DEFAULT_RECONNECT_PERIOD;
        } else if ("false".equalsIgnoreCase(param)) {
            reconnect = 0;
        } else {
            try {
                reconnect = Integer.parseInt(param);
            } catch (Exception e) {
                throw new IllegalArgumentException("reconnect param must be nonnegative integer or false/true. input is:" + param);
            }
            if (reconnect < 0) {
                throw new IllegalArgumentException("reconnect param must be nonnegative integer or false/true. input is:" + param);
            }
        }
        return reconnect;
    }

    public void disconnect() {
        connectLock.lock();
        try {
            destroyConnectStatusCheckCommand();
            try {
                Channel channel = getChannel();
                if (channel != null) {
                    channel.close();
                }
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
            }
            try {
                doDisConnect();
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
            }
        } finally {
            connectLock.unlock();
        }
    }
    private synchronized void destroyConnectStatusCheckCommand() {
        try {
            if (reconnectExecutorFuture != null && !reconnectExecutorFuture.isDone()) {
                reconnectExecutorFuture.cancel(true);
                reconnectExecutorService.purge();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    /** 重置连接：先断开连接，在尝试连接 */
    public void reconnect() throws RemotingException {
        disconnect();
        connect();
    }

    public void close(int timeout) {
        ExecutorUtil.gracefulShutdown(executor, timeout);
        close();
    }
    public void close() {
        try {
            if (executor != null) {
                ExecutorUtil.shutdownNow(executor, 100);
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            super.close();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            disconnect();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            doClose();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }






    // ========================================================
    // ===== 子类扩展： MinaClient、NettyClient、GrizzlyClient都实现以下方法
    // ========================================================

    /** 打开（创建）客户端 */
    protected abstract void doOpen() throws Throwable;
    /** 关闭客户端 */
    protected abstract void doClose() throws Throwable;
    /** 连接到服务端 */
    protected abstract void doConnect() throws Throwable;
    /** 与服务端断开连接 */
    protected abstract void doDisConnect() throws Throwable;
    /** 获取用于数据传输的连接通道 */
    protected abstract Channel getChannel();


    @Override
    public String toString() {
        return getClass().getName() + " [" + getLocalAddress() + " -> " + getRemoteAddress() + "]";
    }

}
