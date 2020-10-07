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

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.ChannelHandler;

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

/**
 * MinaHandler：用于处理客户端请求
 */
public class MinaHandler extends IoHandlerAdapter {

    private final URL url;

    private final ChannelHandler handler;

    public MinaHandler(URL url, ChannelHandler handler) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        this.url = url;
        this.handler = handler;
    }

    /**
     * 当session打开时（即服务端和客户端建立连接时）触发ChannelHandler.connected方法，
     *
     * @param session
     * @throws Exception
     */
    @Override
    public void sessionOpened(IoSession session) throws Exception {
        MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
        try {
            handler.connected(channel);
        } finally {
            MinaChannel.removeChannelIfDisconnectd(session);
        }
    }

    /**
     * 当session关闭时（即服务端和客户端的连接断开时）调用
     *
     * @param session
     * @throws Exception
     */
    @Override
    public void sessionClosed(IoSession session) throws Exception {
        MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
        try {
            handler.disconnected(channel);
        } finally {
            MinaChannel.removeChannelIfDisconnectd(session);
        }
    }

    /**
     * 当接收了一个客户端请求时调用该方法
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
        try {
            // 处理请求
            handler.received(channel, message);
        } finally {
            MinaChannel.removeChannelIfDisconnectd(session);
        }
    }

    /**
     * 当一个消息被(IoSession#write)发送出去后调用，即服务端发起响应后的回调
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
        try {
            handler.sent(channel, message);
        } finally {
            MinaChannel.removeChannelIfDisconnectd(session);
        }
    }

    /**
     * 服务端请求处理异常时，调用该方法
     *
     * @param session
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
        try {
            handler.caught(channel, cause);
        } finally {
            MinaChannel.removeChannelIfDisconnectd(session);
        }
    }

}