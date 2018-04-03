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
package com.alibaba.dubbo.rpc.cluster;

import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.support.FailoverCluster;

/**
 * Cluster. (SPI, Singleton, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Computer_cluster">Cluster</a>
 * <a href="http://en.wikipedia.org/wiki/Fault-tolerant_system">Fault-Tolerant</a>
 *

 Failover Cluster：
    优点：失败自动切换，当出现失败，重试其它服务器，通常用于读操作（推荐使用）
    缺点：重试会带来更长延迟

 Failfast Cluster：
    优点：快速失败，只发起一次调用，失败立即报错,通常用于非幂等性的写操作
    缺点：如果有机器正在重启，可能会出现调用失败

 Failsafe Cluster：
    优点：失败安全，出现异常时，直接忽略，通常用于写入审计日志等操作
    缺点：调用信息丢失

 Failback Cluster：
    优点：失败自动恢复，后台记录失败请求，定时重发，通常用于消息通知操作
    缺点：

 Forking Cluster：
    优点：并行调用多个服务器，只要一个成功即返回，通常用于实时性要求较高的读操作
    缺点：需要浪费更多服务资源

 Broadcast Cluster：
    优点：广播调用所有提供者，逐个调用，任意一台报错则报错，通常用于更新提供方本地状态
    缺点：速度慢，任意一台报错则报错

 */
@SPI(FailoverCluster.NAME)
public interface Cluster {

    /**
     * Merge the directory invokers to a virtual invoker.
     * 将 Directory 中的多个 Invoker 伪装成一个 Invoker, 对上层透明，包含集群的容错机制
     *
     * @param <T>
     * @param directory
     * @return cluster invoker
     * @throws RpcException
     */
    @Adaptive
    <T> Invoker<T> join(Directory<T> directory) throws RpcException;

}