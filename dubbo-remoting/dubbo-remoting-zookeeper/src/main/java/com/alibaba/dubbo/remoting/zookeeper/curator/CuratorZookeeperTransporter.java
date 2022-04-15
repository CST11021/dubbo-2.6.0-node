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
package com.alibaba.dubbo.remoting.zookeeper.curator;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperClient;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperTransporter;

/**
 * 通过Curator创建一个zk客户端
 */
public class CuratorZookeeperTransporter implements ZookeeperTransporter {

    /**
     * 通过url获取对应的zk服务器信息，从而创建一个zk客户端实例
     *
     * @param url   保存了zk服务器的信息
     * @return
     */
    public ZookeeperClient connect(URL url) {
        return new CuratorZookeeperClient(url);
    }

}
