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
package com.alibaba.dubbo.registry;

import com.alibaba.dubbo.common.Node;
import com.alibaba.dubbo.common.URL;

/**
 * Registry. (SPI, Prototype, ThreadSafe)
 *
 * @see com.alibaba.dubbo.registry.RegistryFactory#getRegistry(URL)
 * @see com.alibaba.dubbo.registry.support.AbstractRegistry
 *
1、MulticastRegistry：
    Multicast 注册中心不需要启动任何中心节点，只要广播地址一样，就可以互相发现。
        1. 提供方启动时广播自己的地址
        2. 消费方启动时广播订阅请求
        3. 提供方收到订阅请求时，单播自己的地址给订阅者，如果设置了  unicast=false  ，则广播给订阅者
        4. 消费方收到提供方地址时，连接该地址进行 RPC 调用。

    组播受网络结构限制，只适合小规模应用或开发阶段使用。组播地址段: 224.0.0.0 - 239.255.255.255

    配置：
        <dubbo:registry address="multicast://224.5.6.7:1234" />
        或
        <dubbo:registry protocol="multicast" address="224.5.6.7:1234" />

        为了减少广播量，Dubbo 缺省使用单播发送提供者地址信息给消费者，如果一个机器上同时启了多个消费者进程，消费者需声明
    unicast=false  ，否则只会有一个消费者能收到消息：

        <dubbo:registry address="multicast://224.5.6.7:1234?unicast=false" />
         或
        <dubbo:registry protocol="multicast" address="224.5.6.7:1234">
            <dubbo:parameter key="unicast" value="false" />
        </dubbo:registry>

 2、ZookeeperRegistry：
    流程说明：
        ①服务提供者启动时: 向 /dubbo/com.foo.BarService/providers  目录下写入自己的 URL 地址
        ②服务消费者启动时: 订阅 /dubbo/com.foo.BarService/providers  目录下的提供者 URL 地址。并向  /dubbo/com.foo.BarService/consumers  目录下写入自己的 URL 地址
        ③监控中心启动时: 订阅 /dubbo/com.foo.BarService  目录下的所有提供者和消费者 URL 地址。

    支持以下功能：
        ①当提供者出现断电等异常停机时，注册中心能自动删除提供者信息
        ②当注册中心重启时，能自动恢复注册数据，以及订阅请求
        ③当会话过期时，能自动恢复注册数据，以及订阅请求
        ④当设置  <dubbo:registry check="false" />  时，记录失败注册和订阅请求，后台定时重试
        ⑤可通过  <dubbo:registry username="admin" password="1234" />  设置 zookeeper 登录信息
        ⑥可通过  <dubbo:registry group="dubbo" />  设置 zookeeper 的根节点，不设置将使用无根树
        ⑦支持  *  号通配符  <dubbo:reference group="*" version="*" />  ，可订阅服务的所有分组和所有版本的提供者

 */
public interface Registry extends Node, RegistryService {
}