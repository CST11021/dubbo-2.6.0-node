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
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Server;
import com.alibaba.dubbo.remoting.Transporter;

/**
 * 说明：
 * 服务器端要用 bind() 函数将套接字与特定的IP地址和端口绑定起来，只有这样，流经该IP地址和端口的数据才能交给套接字处理；
 * 而客户端要用 connect() 函数建立连接。
 */
public class MinaTransporter implements Transporter {

    public static final String NAME = "mina";

    /**
     * 绑定服务端口，并启动一个Mina服务
     *
     * @param url       server url
     * @param handler   用于处理请求的Handler
     * @return
     * @throws RemotingException
     */
    public Server bind(URL url, ChannelHandler handler) throws RemotingException {
        return new MinaServer(url, handler);
    }

    /**
     * 与服务端建立连接并返回一个客户端对象
     *
     * @param url     server url
     * @param handler
     * @return
     * @throws RemotingException
     */
    public Client connect(URL url, ChannelHandler handler) throws RemotingException {
        return new MinaClient(url, handler);
    }

}
