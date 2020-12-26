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
package com.alibaba.dubbo.remoting.transport.mina;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.AbstractClient;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mina client.
 */
public class MinaClient extends AbstractClient {

    private static final Logger logger = LoggerFactory.getLogger(MinaClient.class);

    /** 用于保存远端对应的客户端对象，避免重复创建，Map<{@link URL#toFullString}, Mina的客户端（SocketConnector）>*/
    private static final Map<String, SocketConnector> connectors = new ConcurrentHashMap<String, SocketConnector>();

    /** 当前客户端连接的远端（服务端）地址信息，例如：dubbo://localhost:9123 */
    private String connectorKey;

    /** Mina框架中的Socket连接器，它是Mina框架中的客户端对象 */
    private SocketConnector connector;

    /** Mina框架中的连接会话对象 */
    private volatile IoSession session;

    /**
     *
     *
     * @param url
     * @param handler   该ChannelHandler为Transport层的通道IO事件处理器
     * @throws RemotingException
     */
    public MinaClient(final URL url, final ChannelHandler handler) throws RemotingException {
        super(url, wrapChannelHandler(url, handler));
    }

    /**
     * 使用Mina框架创建一个客户端，注意，这里仅仅只是创建Mina的客户端对象，并不发起连接服务端请求，连接服务端请求的动作在{@link #doConnect()}方法中实现
     *
     * @throws Throwable
     */
    @Override
    protected void doOpen() throws Throwable {
        connectorKey = getUrl().toFullString();
        SocketConnector c = connectors.get(connectorKey);
        if (c != null) {
            connector = c;
        } else {
            // set thread pool.
            connector = new SocketConnector(Constants.DEFAULT_IO_THREADS, Executors.newCachedThreadPool(new NamedThreadFactory("MinaClientWorker", true)));

            // config
            SocketConnectorConfig cfg = (SocketConnectorConfig) connector.getDefaultConfig();
            cfg.setThreadModel(ThreadModel.MANUAL);
            cfg.getSessionConfig().setTcpNoDelay(true);
            cfg.getSessionConfig().setKeepAlive(true);
            int timeout = getTimeout();
            cfg.setConnectTimeout(timeout < 1000 ? 1 : timeout / 1000);
            // set codec.
            connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MinaCodecAdapter(getCodec(), getUrl(), this)));
            connectors.put(connectorKey, connector);
        }
    }

    /**
     * 客户端发起连接远端的请求，并阻塞知道连接到服务端
     *
     * @throws Throwable
     */
    @Override
    protected void doConnect() throws Throwable {
        // 向指定服务端发起请求
        ConnectFuture future = connector.connect(getConnectAddress(), new MinaHandler(getUrl(), this));
        long start = System.currentTimeMillis();
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        // resolve future.awaitUninterruptibly() dead lock
        final CountDownLatch finish = new CountDownLatch(1);
        future.addListener(new IoFutureListener() {

            public void operationComplete(IoFuture future) {

                try {
                    // 判断异步操作是否完成，一般走到这里返回的都是true
                    if (future.isReady()) {
                        IoSession newSession = future.getSession();
                        try {
                            // Close old channel
                            // copy reference
                            IoSession oldSession = MinaClient.this.session;
                            if (oldSession != null) {
                                try {
                                    if (logger.isInfoEnabled()) {
                                        logger.info("Close old mina channel " + oldSession + " on create new mina channel " + newSession);
                                    }
                                    oldSession.close();
                                } finally {
                                    MinaChannel.removeChannelIfDisconnectd(oldSession);
                                }
                            }
                        } finally {
                            if (MinaClient.this.isClosed()) {
                                try {
                                    if (logger.isInfoEnabled()) {
                                        logger.info("Close new mina channel " + newSession + ", because the client closed.");
                                    }
                                    newSession.close();
                                } finally {
                                    MinaClient.this.session = null;
                                    MinaChannel.removeChannelIfDisconnectd(newSession);
                                }
                            } else {
                                MinaClient.this.session = newSession;
                            }
                        }
                    }
                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    finish.countDown();
                }

            }

        });

        // 阻塞等待，直到连接上服务端
        try {
            finish.await(getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RemotingException(this, "client(url: " + getUrl() + ") failed to connect to server " + getRemoteAddress() + " client-side timeout "
                    + getTimeout() + "ms (elapsed: " + (System.currentTimeMillis() - start)
                    + "ms) from netty client " + NetUtils.getLocalHost() + " using dubbo version "
                    + Version.getVersion() + ", cause: " + e.getMessage(), e);
        }

        Throwable e = exception.get();
        if (e != null) {
            throw e;
        }
    }

    /**
     * 连接断开的处理动作
     *
     * @throws Throwable
     */
    @Override
    protected void doDisConnect() throws Throwable {
        try {
            // TODO whz 为什么这里要移除？
            MinaChannel.removeChannelIfDisconnectd(session);
        } catch (Throwable t) {
            logger.warn(t.getMessage());
        }
    }

    @Override
    protected void doClose() throws Throwable {
        //release mina resouces.
    }

    @Override
    protected Channel getChannel() {
        IoSession s = session;
        if (s == null || !s.isConnected())
            return null;
        return MinaChannel.getOrAddChannel(s, getUrl(), this);
    }

}