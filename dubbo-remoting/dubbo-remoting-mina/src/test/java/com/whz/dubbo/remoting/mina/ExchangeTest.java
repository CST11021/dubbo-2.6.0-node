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
package com.whz.dubbo.remoting.mina;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.*;
import com.alibaba.dubbo.remoting.exchange.support.Replier;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * MinaServerClientTest
 */
public class ExchangeTest extends TestCase {

    private int port = 9123;

    protected ExchangeServer server;

    protected ExchangeClient client;

    /** 用于处理客户端的请求 */
    protected RequestReplier replier = new RequestReplier();

    /** 客户端与服务端的通道事件监听 */
    protected WhzChannelHandler channelHandler = new WhzChannelHandler();


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        server = Exchangers.bind(URL.valueOf("exchange://localhost:" + port + "?server=mina"), channelHandler, replier);
        client = Exchangers.connect(URL.valueOf("exchange://localhost:" + port + "?client=mina"), channelHandler, replier);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        try {
            if (server != null) {
                server.close();
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    @Test
    public void testFuture() throws Exception {

        // 客户端想服务端发送请求
        ResponseFuture future = client.request("world");
        System.out.println(future.get());

        // 向客户端发送消息
        server.send("hello，我是server");

    }

    public class RequestReplier implements Replier<Object> {

        public Class<Object> interest() {
            return Object.class;
        }

        public Object reply(ExchangeChannel channel, Object msg) throws RemotingException {
            System.out.println("收到通道消息，msg = " + msg.toString());
            return "<" + msg + ">消息已收到";
        }

    }

    public class WhzChannelHandler implements ChannelHandler {

        @Override
        public void connected(Channel channel) throws RemotingException {
            System.out.println("当客户端与服务端建立连接时调用");
        }

        @Override
        public void disconnected(Channel channel) throws RemotingException {
            System.out.println("当客户端与服务端断开连接时调用");
        }

        @Override
        public void sent(Channel channel, Object message) throws RemotingException {
            System.out.println("当向通道发送消息的时候调用，message = " + message.toString());
        }

        @Override
        public void received(Channel channel, Object message) throws RemotingException {
            System.out.println("当从通道接收到消息的时候调用，message = " + message.toString());
        }

        @Override
        public void caught(Channel channel, Throwable exception) throws RemotingException {
            System.out.println("异常回调：" + exception.getMessage());
        }

    }

}