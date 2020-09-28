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
package com.alibaba.dubbo.remoting.zookeeper;

import com.alibaba.dubbo.common.URL;

import java.util.List;

/**
 * dubbo中用于操作zk的客户端接口
 */
public interface ZookeeperClient {

    /**
     * 用于获取zk服务端信息
     *
     * @return
     */
    URL getUrl();

    /**
     * 创建节点
     *
     * @param path          节点路径
     * @param ephemeral     是否临时节点
     */
    void create(String path, boolean ephemeral);

    /**
     * 删除节点
     *
     * @param path
     */
    void delete(String path);

    /**
     * 获取路径下的子节点
     *
     * @param path
     * @return
     */
    List<String> getChildren(String path);

    /**
     * 监听子节点
     *
     * @param path
     * @param listener
     * @return
     */
    List<String> addChildListener(String path, ChildListener listener);

    /**
     * 移除子节点监听
     *
     * @param path
     * @param listener
     */
    void removeChildListener(String path, ChildListener listener);

    /**
     * 添加状态监听
     *
     * @param listener
     */
    void addStateListener(StateListener listener);

    /**
     * 移除zk状态的监听
     *
     * @param listener
     */
    void removeStateListener(StateListener listener);

    /**
     * zk客户端是否与服务端处于连接状态
     *
     * @return
     */
    boolean isConnected();

    /**
     * 关闭客户端
     */
    void close();

}
