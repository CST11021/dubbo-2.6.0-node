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

package com.alibaba.dubbo.common.store;

import com.alibaba.dubbo.common.extension.SPI;

import java.util.Map;

@SPI("simple")
public interface DataStore {

    /**
     * 根据组件名，获取组件下的缓存数据
     *
     * @param componentName
     * @return
     */
    Map<String, Object> get(String componentName);

    /**
     * 根据组件名和缓存key，获取缓存数据
     *
     * @param componentName
     * @param key
     * @return
     */
    Object get(String componentName, String key);

    /**
     * 添加缓存
     *
     * @param componentName
     * @param key
     * @param value
     */
    void put(String componentName, String key, Object value);

    /**
     * 移除缓存
     *
     * @param componentName
     * @param key
     */
    void remove(String componentName, String key);

}
