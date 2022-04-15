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
package com.alibaba.dubbo.remoting.p2p;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;

/**
 * 用于维护一组服务器节点
 * Group. (SPI, Prototype, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Peer-to-peer">Peer-to-peer</a>
 */
public interface Group {

    /**
     * 表示维护一组服务器集合的配置，可能是一个配置文件，或者是网络组播对象
     *
     * @return 服务组的信息
     */
    URL getUrl();

    /**
     * 将一个节点添加到组，并返回一个代表服务组的Peer对象
     *
     * @param url
     */
    Peer join(URL url, ChannelHandler handler) throws RemotingException;

    /**
     * 将一个节点从该服务组移除
     *
     * @param url
     * @throws RemotingException
     */
    void leave(URL url) throws RemotingException;

    /**
     * 关闭该服务组
     */
    void close();

}