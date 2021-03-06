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
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Endpoint;
import com.alibaba.dubbo.remoting.RemotingException;

/** AbstractPeer：用于描述通道对应的ChannelHandler和通道连接状态 */
public abstract class AbstractPeer implements Endpoint, ChannelHandler {

    /** 收发消息都是基于通道，该组件用于触发通道相关的事件 */
    private final ChannelHandler handler;
    /** 表示机器节点的URL */
    private volatile URL url;
    /** 表示进程正在关闭或关闭已完成 */
    private volatile boolean closing;
    /** 为true表示通道已经关闭 */
    private volatile boolean closed;


    public AbstractPeer(URL url, ChannelHandler handler) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        this.url = url;
        this.handler = handler;
    }



    // 关于通道状态

    public boolean isClosed() {
        return closed;
    }
    public boolean isClosing() {
        return closing && !closed;
    }
    public void close(int timeout) {
        close();
    }
    public void close() {
        closed = true;
    }
    public void startClose() {
        if (isClosed()) {
            return;
        }
        closing = true;
    }

    // URL

    public URL getUrl() {
        return url;
    }
    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        this.url = url;
    }


    // 消息通道处理器

    /**
     * 获取通道事件监听处理器
     *
     * @return
     */
    public ChannelHandler getChannelHandler() {
        if (handler instanceof ChannelHandlerDelegate) {
            return ((ChannelHandlerDelegate) handler).getHandler();
        } else {
            return handler;
        }
    }
    @Deprecated
    public ChannelHandler getHandler() {
        return getDelegateHandler();
    }
    /**
     * Return the final handler (which may have been wrapped). This method should be distinguished with getChannelHandler() method
     *
     * @return ChannelHandler
     */
    public ChannelHandler getDelegateHandler() {
        return handler;
    }

    /**
     * 向通道发送消息
     *
     * @param message
     * @throws RemotingException
     */
    public void send(Object message) throws RemotingException {
        send(message, url.getParameter(Constants.SENT_KEY, false));
    }










    // ========= 实现ChannelHandler接口：通道IO事件处理接口，全部委托给#handler处理 =========


    /**
     * 当客户端与服务端建立通道连接时，调用该方法
     *
     * @param ch channel.
     */
    public void connected(Channel ch) throws RemotingException {
        if (closed) {
            return;
        }
        handler.connected(ch);
    }

    /**
     * 当客户端与服务端的通道连接断开时，调用该方法
     *
     * @param ch channel.
     */
    public void disconnected(Channel ch) throws RemotingException {
        handler.disconnected(ch);
    }

    /**
     * 向Channel发送一个消息，委托给ChannelHandler实现
     *
     * @param ch 用于发送消息的通道
     * @param msg 要发送的消息
     */
    public void sent(Channel ch, Object msg) throws RemotingException {
        if (closed) {
            return;
        }
        handler.sent(ch, msg);
    }

    /**
     * 当监听到通道有消息发送过来时会调用该方法
     *
     * @param ch
     * @param msg
     * @throws RemotingException
     */
    public void received(Channel ch, Object msg) throws RemotingException {
        if (closed) {
            return;
        }
        handler.received(ch, msg);
    }

    /**
     * 异常时调用该方法
     *
     * @param ch
     * @param ex
     * @throws RemotingException
     */
    public void caught(Channel ch, Throwable ex) throws RemotingException {
        handler.caught(ch, ex);
    }
}