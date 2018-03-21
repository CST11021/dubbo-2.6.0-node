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
package com.alibaba.dubbo.remoting;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;
import com.alibaba.dubbo.remoting.transport.dispatcher.all.AllDispatcher;

/**
 * ChannelHandlerWrapper (SPI, Singleton, ThreadSafe)
 *
 * 根据URL参数dispatcher选择Dispatcher类，不同的Dispatcher创建不同的Handler，表示通道的事件的处理模式，在这些Handler中构
 * 建异步执行任务ChannelEventRunnable，异步执行connected、disconnected、received、caught、sent等操作。目前支持四种Handler处理器：
 （1）AllChannelHandler：除发送消息之外，所有的事件都采用线程池处理。
 （2）ExecutionChannelHandler：与AllChannelHandler不同之处，若创建的线程池ExecutorService不可用，AllChannelHandler将使用
      共享线程池，而ExecutionChannelHandler只有报错。
 （3）ChannelHandler：所有事件都真接有channelhandler处理，不采用线程池。
 （4）MessageOnlyChannelHandler：只有接受消息采用线程池。
 （5）ConnectionOrderedChannelHandler：单线程，排队处理。
 在上述的Handler的初始化过程中，会根据url的参数threadpool来创建线程份，目前支持的线程池类有三种，默认FixedThreadPool。

 FixedThreadPool:此线程池启动时即创建固定大小的线程数，不做任何伸缩。
 CachedThreadPool：此线程池可伸缩，线程空闲一分钟后回收，新请求重新创建线程。
 LimitedThreadPool：此线程一直增长，直到上限，增长后不收缩。
 *
 */
@SPI(AllDispatcher.NAME)
public interface Dispatcher {

    /**
     * dispatch the message to threadpool.
     *
     * @param handler
     * @param url
     * @return channel handler
     */
    @Adaptive({Constants.DISPATCHER_KEY, "dispather", "channel.handler"})
    // The last two parameters are reserved for compatibility with the old configuration
    ChannelHandler dispatch(ChannelHandler handler, URL url);

}