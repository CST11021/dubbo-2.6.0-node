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

import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Server;

/**
 * Peer. (SPI, Prototype, ThreadSafe)
 *
 * Peer的英文意思是“同等地位的人”，这里Peer的主要作用是将多个服务提供封装为一个服务对外提供，该接口仅提供了一个leave方法，使每个服务都具有从服务组下线的能力
 *
 * <a href="http://en.wikipedia.org/wiki/Peer-to-peer">Peer-to-peer</a>
 *
 */
public interface Peer extends Server {

    /**
     * 将当前的服务从服务组移除
     *
     * @throws RemotingException
     */
    void leave() throws RemotingException;

}