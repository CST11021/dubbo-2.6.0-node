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

    /** 收发消息都是基于通道，表示通道消息处理 */
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


    // 连接或断开

    public void connected(Channel ch) throws RemotingException {
        if (closed) {
            return;
        }
        handler.connected(ch);
    }
    public void disconnected(Channel ch) throws RemotingException {
        handler.disconnected(ch);
    }


    // 发送或接受消息

    public void send(Object message) throws RemotingException {
        send(message, url.getParameter(Constants.SENT_KEY, false));
    }
    public void sent(Channel ch, Object msg) throws RemotingException {
        if (closed) {
            return;
        }
        handler.sent(ch, msg);
    }
    public void received(Channel ch, Object msg) throws RemotingException {
        if (closed) {
            return;
        }
        handler.received(ch, msg);
    }



    public void caught(Channel ch, Throwable ex) throws RemotingException {
        handler.caught(ch, ex);
    }
}