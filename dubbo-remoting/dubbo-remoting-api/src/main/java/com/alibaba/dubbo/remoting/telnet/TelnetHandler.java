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
package com.alibaba.dubbo.remoting.telnet;

import com.alibaba.dubbo.common.extension.SPI;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;

/**
 * TelnetHandler
 */
@SPI
public interface TelnetHandler {

    /**
     * 当使用 telnet 命令调用dubbo服务时，会将调用该方法处理invoke命令，这里的message就是客户端输入的命令，例如：invoke HelloService.sayHello("whz")
     *
     * @param channel   telnet客户端与服务端的通信通道
     * @param message   具体的命令，例如：invoke HelloService.sayHello("whz")
     * @return 返回命令的执行结果，例如如下：
     * "Hello whz"
     * elapsed: 4 ms.
     * dubbo>
     *
     * @throws RemotingException
     */
    String telnet(Channel channel, String message) throws RemotingException;

}