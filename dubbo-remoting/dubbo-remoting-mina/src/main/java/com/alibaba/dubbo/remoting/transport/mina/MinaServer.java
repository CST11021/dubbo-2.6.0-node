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
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ExecutorUtil;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.AbstractServer;
import com.alibaba.dubbo.remoting.transport.dispatcher.ChannelHandlers;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * MinaServer
 */
public class MinaServer extends AbstractServer {

    private static final Logger logger = LoggerFactory.getLogger(MinaServer.class);

    /** Mina框架的服务端对象 */
    private SocketAcceptor acceptor;

    /**
     * ChannelHandlers.wrap方法很重要，dubbo的线程模型就通过该方法实现的
     *
     * @param url
     * @param handler
     * @throws RemotingException
     */
    public MinaServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, ChannelHandlers.wrap(handler, ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME)));
    }

    /**
     * 启动一个Mina服务，并开始监听客户端请求
     *
     * @throws Throwable
     */
    @Override
    protected void doOpen() throws Throwable {
        // 创建服务端对象，并配置服务端线程池的线程数量
        acceptor = new SocketAcceptor(
                getUrl().getPositiveParameter(Constants.IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS),
                Executors.newCachedThreadPool(new NamedThreadFactory("MinaServerWorker", true))
        );

        // 设置服务端配置
        SocketAcceptorConfig cfg = (SocketAcceptorConfig) acceptor.getDefaultConfig();
        cfg.setThreadModel(ThreadModel.MANUAL);

        // 设置Mina的编解码处理器
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MinaCodecAdapter(getCodec(), getUrl(), this)));

        // 启动Mina服务：绑定端口并监听客户端请求；
        acceptor.bind(getBindAddress(), new MinaHandler(getUrl(), this));
    }

    /**
     * 关闭Mina服务
     *
     * @throws Throwable
     */
    @Override
    protected void doClose() throws Throwable {
        try {
            if (acceptor != null) {
                acceptor.unbind(getBindAddress());
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    /**
     * 获取所有的Channel
     *
     * @return
     */
    public Collection<Channel> getChannels() {
        Set<IoSession> sessions = acceptor.getManagedSessions(getBindAddress());
        Collection<Channel> channels = new HashSet<Channel>();
        for (IoSession session : sessions) {
            if (session.isConnected()) {
                channels.add(MinaChannel.getOrAddChannel(session, getUrl(), this));
            }
        }
        return channels;
    }

    /**
     * 获取连接到指定客户端的通道
     *
     * @param remoteAddress
     * @return
     */
    public Channel getChannel(InetSocketAddress remoteAddress) {
        Set<IoSession> sessions = acceptor.getManagedSessions(getBindAddress());
        for (IoSession session : sessions) {
            if (session.getRemoteAddress().equals(remoteAddress)) {
                return MinaChannel.getOrAddChannel(session, getUrl(), this);
            }
        }
        return null;
    }

    /**
     * 判断服务是否已经开启服务（即绑定到指定IP/端口）
     *
     * @return
     */
    public boolean isBound() {
        return acceptor.isManaged(getBindAddress());
    }

}