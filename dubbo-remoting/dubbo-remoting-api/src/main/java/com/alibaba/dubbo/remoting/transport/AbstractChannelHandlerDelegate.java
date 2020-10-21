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

import com.alibaba.dubbo.common.utils.Assert;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;

public abstract class AbstractChannelHandlerDelegate implements ChannelHandlerDelegate {

    protected ChannelHandler handler;

    protected AbstractChannelHandlerDelegate(ChannelHandler handler) {
        Assert.notNull(handler, "handler == null");
        this.handler = handler;
    }

    public ChannelHandler getHandler() {
        if (handler instanceof ChannelHandlerDelegate) {
            return ((ChannelHandlerDelegate) handler).getHandler();
        }
        return handler;
    }

    /**
     * 当客户端与服务端建立通道连接时，调用该方法
     *
     * @param channel channel.
     */
    public void connected(Channel channel) throws RemotingException {
        handler.connected(channel);
    }

    /**
     * 当客户端与服务端的通道连接断开时，调用该方法
     *
     * @param channel channel.
     */
    public void disconnected(Channel channel) throws RemotingException {
        handler.disconnected(channel);
    }

    /**
     * 向Channel发送一个消息时，调用该方法
     *
     * @param channel 用于发送消息的通道
     * @param message 要发送的消息
     */
    public void sent(Channel channel, Object message) throws RemotingException {
        handler.sent(channel, message);
    }

    /**
     * 当接收到请求时调用该方法
     *
     * @param channel 用于接收消息的通道.
     * @param message 要接收的消息.
     */
    public void received(Channel channel, Object message) throws RemotingException {
        handler.received(channel, message);
    }

    public void caught(Channel channel, Throwable exception) throws RemotingException {
        handler.caught(channel, exception);
    }
}
