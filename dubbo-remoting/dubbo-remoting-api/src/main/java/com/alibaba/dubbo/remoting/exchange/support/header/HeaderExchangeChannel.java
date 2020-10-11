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
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;
import com.alibaba.dubbo.remoting.exchange.support.DefaultFuture;

import java.net.InetSocketAddress;

/**
 * ExchangeReceiver
 */
final class HeaderExchangeChannel implements ExchangeChannel {

    private static final Logger logger = LoggerFactory.getLogger(HeaderExchangeChannel.class);

    private static final String CHANNEL_KEY = HeaderExchangeChannel.class.getName() + ".CHANNEL";

    /** 用于通信的通道，具备基础的客户端通信能力（单向通信，不具备Request/response语义） */
    private final Channel channel;

    private volatile boolean closed = false;

    HeaderExchangeChannel(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel == null");
        }
        this.channel = channel;
    }

    static HeaderExchangeChannel getOrAddChannel(Channel ch) {
        if (ch == null) {
            return null;
        }

        HeaderExchangeChannel ret = (HeaderExchangeChannel) ch.getAttribute(CHANNEL_KEY);
        if (ret == null) {
            ret = new HeaderExchangeChannel(ch);
            if (ch.isConnected()) {
                ch.setAttribute(CHANNEL_KEY, ret);
            }
        }
        return ret;
    }

    static void removeChannelIfDisconnected(Channel ch) {
        if (ch != null && !ch.isConnected()) {
            ch.removeAttribute(CHANNEL_KEY);
        }
    }


    // send方法中将发送message统一封装为Request后，再发送，但send依然只是单向通信
    // request方法才具备Request/response语义

    /**
     * 向通道发送一个消息，可能是客户端向服务端发起请求，也可能是服务端响应客户端请求
     *
     * @param message
     * @throws RemotingException
     */
    public void send(Object message) throws RemotingException {
        send(message, getUrl().getParameter(Constants.SENT_KEY, false));
    }
    /**
     * 向所有的通道发送消息
     *
     * @param message
     * @param sent    already sent to socket?
     * @throws RemotingException
     */
    public void send(Object message, boolean sent) throws RemotingException {
        if (closed) {
            throw new RemotingException(this.getLocalAddress(), null, "Failed to send message " + message + ", cause: The channel " + this + " is closed!");
        }

        if (message instanceof Request || message instanceof Response || message instanceof String) {
            channel.send(message, sent);
        } else {
            Request request = new Request();
            request.setVersion("2.0.0");
            request.setTwoWay(false);
            request.setData(message);
            channel.send(request, sent);
        }
    }

    /**
     * 发送请求，并返回一个响应信息
     *
     * @param request
     * @return response future
     * @throws RemotingException
     */
    public ResponseFuture request(Object request) throws RemotingException {
        return request(request, channel.getUrl().getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
    }
    public ResponseFuture request(Object request, int timeout) throws RemotingException {
        if (closed) {
            throw new RemotingException(this.getLocalAddress(), null, "Failed to send request " + request + ", cause: The channel " + this + " is closed!");
        }

        // create request.
        Request req = new Request();
        req.setVersion("2.0.0");
        req.setTwoWay(true);
        req.setData(request);
        DefaultFuture future = new DefaultFuture(channel, req, timeout);
        try {
            channel.send(req);
        } catch (RemotingException e) {
            future.cancel();
            throw e;
        }
        return future;
    }



    // 以下实现都是委托式的实现

    /**
     * 判断通道是否关闭
     *
     * @return
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * 关闭通道
     */
    public void close() {
        try {
            channel.close();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    /**
     * 平滑关闭通道
     *
     * @param timeout
     */
    public void close(int timeout) {
        if (closed) {
            return;
        }
        closed = true;
        if (timeout > 0) {
            long start = System.currentTimeMillis();
            while (DefaultFuture.hasFuture(channel)
                    && System.currentTimeMillis() - start < timeout) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
        close();
    }

    /** 开始关闭通道 */
    @Override
    public void startClose() {
        channel.startClose();
    }

    /**
     * 获取创建通道的所在节点的地址
     *
     * @return
     */
    public InetSocketAddress getLocalAddress() {
        return channel.getLocalAddress();
    }

    /**
     * 获取通道连接的远程地址
     *
     * @return
     */
    public InetSocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }

    /**
     * 获取创建通道的URL信息（通道所在节点的ip/端口）
     *
     * @return
     */
    public URL getUrl() {
        return channel.getUrl();
    }

    /**
     * 通道是否已经建立连接
     *
     * @return
     */
    public boolean isConnected() {
        return channel.isConnected();
    }

    /**
     * 获取通道事件处理器
     *
     * @return
     */
    public ChannelHandler getChannelHandler() {
        return channel.getChannelHandler();
    }

    /**
     * Exchange通道事件处理器
     *
     * @return
     */
    public ExchangeHandler getExchangeHandler() {
        return (ExchangeHandler) channel.getChannelHandler();
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



    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        HeaderExchangeChannel other = (HeaderExchangeChannel) obj;
        if (channel == null) {
            if (other.channel != null) return false;
        } else if (!channel.equals(other.channel)) return false;
        return true;
    }

    @Override
    public String toString() {
        return channel.toString();
    }

}