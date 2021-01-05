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

    /** 表示该客户端最后一次成功连接的时间 */
    private long lastConnectedTime = System.currentTimeMillis();
    /** 客户端通过该线程池定时去检查连接是否断开，如果连接断开了，则发起重连，否则重置最后连接时间（对应lastConnectedTime参数） */
    private static final ScheduledThreadPoolExecutor reconnectExecutorService = new ScheduledThreadPoolExecutor(2, new NamedThreadFactory("DubboClientReconnectTimer", true));

    /** 当连接断开时，通过该计数器统计当前尝试重连的失败次数，如果成功连接上了该计数器置为0 */
    private final AtomicInteger reconnect_count = new AtomicInteger(0);
    /** 当连接断开时，如果重连失败了不会立即打印日志，比如该值为2，则表示每两次发起重连失败了才打印一次日志 */
    private final int reconnect_warning_period;
    /** 标记是否已经打印了重连失败的日志，每次成功打印了，设置true */
    private final AtomicBoolean reconnect_error_log_flag = new AtomicBoolean(false);

    /** 表示在ScheduledExecutorService中提交了任务的返回结果，我们通过Delayed的接口getDelay()方法知道该任务还有多久才被执行 */
    private volatile ScheduledFuture<?> reconnectExecutorFuture = null;

    /** 客户端与服务端建立连接时用的锁 */
    private final Lock connectLock = new ReentrantLock();

    /** 表示客户端向服务端发起请求时，是否要重新建立连接 */
    private final boolean send_reconnect;



    private final long shutdown_timeout;
    /** 表示处理通道IO事件的线程池，参考：{@link } */
    protected volatile ExecutorService executor;



    public AbstractClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);

        send_reconnect = url.getParameter(Constants.SEND_RECONNECT_KEY, false);
        shutdown_timeout = url.getParameter(Constants.SHUTDOWN_TIMEOUT_KEY, Constants.DEFAULT_SHUTDOWN_TIMEOUT);
        // 默认的重新连接间隔为2s，1800表示警告间隔为1小时
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

        // 从缓存组件里获取该线程池
        executor = (ExecutorService) ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension().get(Constants.CONSUMER_SIDE, Integer.toString(url.getPort()));
        // 移除处理通道IO事件的线程池
        /** 详见：{@link com.alibaba.dubbo.remoting.transport.dispatcher.WrappedChannelHandler#executor} */
        ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension().remove(Constants.CONSUMER_SIDE, Integer.toString(url.getPort()));
    }

    /**
     * 给URL设置处理通道IO事件的线程名和线程池类，并通过SPI机制创建一个ChannelHandler实例
     *
     * @param url
     * @param handler
     * @return
     */
    protected static ChannelHandler wrapChannelHandler(URL url, ChannelHandler handler) {
        // 给url添加名为"threadname"的参数，默认：DubboClientHandler
        url = ExecutorUtil.setThreadName(url, CLIENT_THREAD_POOL_NAME);
        // 给url添加名为"threadpool"的参数，默认：cached
        url = url.addParameterIfAbsent(Constants.THREADPOOL_KEY, Constants.DEFAULT_CLIENT_THREADPOOL);
        // 根据SPI机制获取一个ChannelHandler实例
        return ChannelHandlers.wrap(handler, url);
    }

    /**
     * 创建一个线程池：线程名为：DubboClientHandler + ${自增的线程池ID} + "-" + "该客户端连接的服务IP地址"
     *
     * @return
     */
    protected ExecutorService createExecutor() {
        return Executors.newCachedThreadPool(new NamedThreadFactory(CLIENT_THREAD_POOL_NAME + CLIENT_THREAD_POOL_ID.incrementAndGet() + "-" + getUrl().getAddress(), true));
    }

    /**
     * 获取当前客户端连接的应用服务地址（该地址可能对应多个服务节点）
     *
     * @return
     */
    public InetSocketAddress getConnectAddress() {
        return new InetSocketAddress(NetUtils.filterLocalHost(getUrl().getHost()), getUrl().getPort());
    }

    /**
     * 获取通道连接的服务节点的IP地址和端口信息
     *
     * @return
     */
    public InetSocketAddress getRemoteAddress() {
        Channel channel = getChannel();
        if (channel == null)
            return getUrl().toInetSocketAddress();
        return channel.getRemoteAddress();
    }

    /**
     * 获取当前客户端的本地地址
     *
     * @return
     */
    public InetSocketAddress getLocalAddress() {
        Channel channel = getChannel();
        if (channel == null)
            return InetSocketAddress.createUnresolved(NetUtils.getLocalHost(), 0);
        return channel.getLocalAddress();
    }


    /**
     * 委托为Channel去实现，底层依赖通信框架的连接Attribute机制
     *
     * @param key
     * @return
     */
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
        // 每次发送消息时，如果连接断开，并且配置send_reconnect为true，则重新建立连接
        if (send_reconnect && !isConnected()) {
            connect();
        }

        // 获取当前客户端对应的通道实例，一般不会为null，如果为null就有问题了
        Channel channel = getChannel();
        //TODO getChannel()返回的值可以为null吗？需要改进
        if (channel == null || !channel.isConnected()) {
            throw new RemotingException(this, "message can not send, because channel is closed . url:" + getUrl());
        }

        // 通过channel来发送消息
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

            // 建立连接，交给底层具体的通信框架去实现
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
        // reconnect表示多久检查一次当前连接是否断开
        int reconnect = getReconnectParam(getUrl());

        // reconnectExecutorFuture 不为null并且还没有canneled，说明当前正在执行重连任务，并且任务还没有执行结束
        if (reconnect > 0 && (reconnectExecutorFuture == null || reconnectExecutorFuture.isCancelled())) {

            // 如果连接断开了，则重新建立连接，否则重置最后连接时间
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

                        // 每隔多少次连接失败，打印一行warn日志
                        if (reconnect_count.getAndIncrement() % reconnect_warning_period == 0) {
                            logger.warn(errorMsg, t);
                        }

                    }
                }
            };

            // 定时任务：如果连接断开了，则发起重连
            reconnectExecutorFuture = reconnectExecutorService.scheduleWithFixedDelay(connectStatusCheckCommand, reconnect, reconnect, TimeUnit.MILLISECONDS);
        }
    }
    /**
     * 获取多久检查一次当前连接是否断开，单位是毫秒，返回0表示不检查
     *
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
        // 优雅的关闭线程池
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
