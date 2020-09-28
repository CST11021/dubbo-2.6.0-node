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
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.remoting.exchange.Exchangers;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;
import com.alibaba.dubbo.remoting.exchange.support.Replier;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * MinaServerClientTest
 */
public class MinaClientToServerTest extends TestCase {

    private int port = 9123;

    protected ExchangeServer server;

    protected ExchangeChannel client;

    protected WorldHandler handler = new WorldHandler();

    protected void setUp() throws Exception {
        super.setUp();
        server = Exchangers.bind(URL.valueOf("exchange://localhost:" + port + "?server=mina"), handler);
        client = Exchangers.connect(URL.valueOf("exchange://localhost:" + port + "?client=mina"));
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

        ResponseFuture future = client.request("world");
        System.out.println(future.get());
    }

    public class WorldHandler implements Replier<Object> {

        public Class<Object> interest() {
            return Object.class;
        }

        public Object reply(ExchangeChannel channel, Object msg) throws RemotingException {
            return "hello " + msg;
        }

    }

}