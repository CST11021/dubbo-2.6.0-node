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
package com.alibaba.dubbo.remoting.transport.dispatcher;


import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Dispatcher;
import com.alibaba.dubbo.remoting.exchange.support.header.HeartbeatHandler;
import com.alibaba.dubbo.remoting.transport.MultiMessageHandler;

/**
 * 这个工具类很重要，dubbo的线程模型的实现机制就是通过改工具类作为切入点实现的
 */
public class ChannelHandlers {

    private static ChannelHandlers INSTANCE = new ChannelHandlers();

    protected ChannelHandlers() {
    }

    public static ChannelHandler wrap(ChannelHandler handler, URL url) {
        return ChannelHandlers.getInstance().wrapInternal(handler, url);
    }

    protected static ChannelHandlers getInstance() {
        return INSTANCE;
    }

    static void setTestingChannelHandlers(ChannelHandlers instance) {
        INSTANCE = instance;
    }

    /**
     * 根据SPI机制获取一个ChannelHandler实例：该实例通过装饰器模式，增加了ChannelHandler的能力：
     * 1、首先入参过来的handler具备了处理实际业务请求的能力，参见：DubboProtocol#requestHandler 实现
     * 2、MultiMessageHandler：增加了消息批处理能力
     * 3、HeartbeatHandler：增加了心跳机制
     * 4、增加了请求异步分发到线程池的机制，参见：AllChannelHandler、ConnectionOrderedChannelHandler、ExecutionChannelHandler、MessageOnlyChannelHandler
     *
     * @param handler
     * @param url
     * @return
     */
    protected ChannelHandler wrapInternal(ChannelHandler handler, URL url) {
        return new MultiMessageHandler(
                new HeartbeatHandler(
                    // 通过SPI自适应机制，获取一个具有路由分发功能的ChannelHandler
                    ExtensionLoader.getExtensionLoader(Dispatcher.class).getAdaptiveExtension().dispatch(handler, url)
                )
        );
    }
}
