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
package com.alibaba.dubbo.rpc.cluster.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;

import java.util.HashMap;
import java.util.Map;

/**
 * ClusterUtils
 *
 */
public class ClusterUtils {

    private ClusterUtils() {
    }

    /**
     * 合并配置
     *
     * @param remoteUrl
     * @param localMap
     * @return
     */
    public static URL mergeUrl(URL remoteUrl, Map<String, String> localMap) {
        Map<String, String> map = new HashMap<String, String>();

        Map<String, String> remoteMap = remoteUrl.getParameters();

        if (remoteMap != null && remoteMap.size() > 0) {
            map.putAll(remoteMap);

            // 从提供程序中删除配置，某些项目应受到提供程序的影响。

            // 移除：threadname、default.threadname
            map.remove(Constants.THREAD_NAME_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.THREAD_NAME_KEY);

            // 移除：threadpool、default.threadpool
            map.remove(Constants.THREADPOOL_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.THREADPOOL_KEY);

            // 移除：corethreads、default.corethreads
            map.remove(Constants.CORE_THREADS_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.CORE_THREADS_KEY);

            // 移除：threads、default.threads
            map.remove(Constants.THREADS_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.THREADS_KEY);

            // 移除：queues、default.queues
            map.remove(Constants.QUEUES_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.QUEUES_KEY);

            // 移除：alive、default.alive
            map.remove(Constants.ALIVE_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.ALIVE_KEY);

            // 移除：transporter、default.transporter
            map.remove(Constants.TRANSPORTER_KEY);
            map.remove(Constants.DEFAULT_KEY_PREFIX + Constants.TRANSPORTER_KEY);
        }

        if (localMap != null && localMap.size() > 0) {
            map.putAll(localMap);
        }


        if (remoteMap != null && remoteMap.size() > 0) {
            // 添加dubbo的版本
            String dubbo = remoteMap.get(Constants.DUBBO_VERSION_KEY);
            if (dubbo != null && dubbo.length() > 0) {
                map.put(Constants.DUBBO_VERSION_KEY, dubbo);
            }
            // 添加version
            String version = remoteMap.get(Constants.VERSION_KEY);
            if (version != null && version.length() > 0) {
                map.put(Constants.VERSION_KEY, version);
            }
            // 添加group
            String group = remoteMap.get(Constants.GROUP_KEY);
            if (group != null && group.length() > 0) {
                map.put(Constants.GROUP_KEY, group);
            }
            // 添加methods
            String methods = remoteMap.get(Constants.METHODS_KEY);
            if (methods != null && methods.length() > 0) {
                map.put(Constants.METHODS_KEY, methods);
            }
            // 添加remote.timestamp
            String remoteTimestamp = remoteMap.get(Constants.TIMESTAMP_KEY);
            if (remoteTimestamp != null && remoteTimestamp.length() > 0) {
                map.put(Constants.REMOTE_TIMESTAMP_KEY, remoteMap.get(Constants.TIMESTAMP_KEY));
            }

            // 在提供者和消费者上组合过滤器和侦听器
            String remoteFilter = remoteMap.get(Constants.REFERENCE_FILTER_KEY);
            String localFilter = localMap.get(Constants.REFERENCE_FILTER_KEY);
            if (remoteFilter != null && remoteFilter.length() > 0 && localFilter != null && localFilter.length() > 0) {
                // reference.filter
                localMap.put(Constants.REFERENCE_FILTER_KEY, remoteFilter + "," + localFilter);
            }

            String remoteListener = remoteMap.get(Constants.INVOKER_LISTENER_KEY);
            String localListener = localMap.get(Constants.INVOKER_LISTENER_KEY);
            if (remoteListener != null && remoteListener.length() > 0 && localListener != null && localListener.length() > 0) {
                // invoker.listener
                localMap.put(Constants.INVOKER_LISTENER_KEY, remoteListener + "," + localListener);
            }
        }

        return remoteUrl.clearParameters().addParameters(map);
    }

}