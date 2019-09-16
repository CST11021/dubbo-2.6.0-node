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
package com.alibaba.dubbo.rpc.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;

public class ProtocolUtils {

    private ProtocolUtils() {
    }

    /**
     * 根据url里参数信息，返回一个服务的key，每个key对应一个{@link com.alibaba.dubbo.rpc.Exporter}
     *
     * @param url
     * @return
     */
    public static String serviceKey(URL url) {
        return serviceKey(url.getPort(), url.getPath(), url.getParameter(Constants.VERSION_KEY),
                url.getParameter(Constants.GROUP_KEY));
    }

    /**
     * {serviceGroup}/{serviceName}:{serviceVersion}:{port}
     * 例如：com.alibaba.dubbo.demo.DemoService:20880，serviceGroup和serviceVersion可选
     *
     * @param port
     * @param serviceName
     * @param serviceVersion
     * @param serviceGroup
     * @return
     */
    public static String serviceKey(int port, String serviceName, String serviceVersion, String serviceGroup) {
        StringBuilder buf = new StringBuilder();
        if (serviceGroup != null && serviceGroup.length() > 0) {
            buf.append(serviceGroup);
            buf.append("/");
        }
        buf.append(serviceName);
        if (serviceVersion != null && serviceVersion.length() > 0 && !"0.0.0".equals(serviceVersion)) {
            buf.append(":");
            buf.append(serviceVersion);
        }
        buf.append(":");
        buf.append(port);
        return buf.toString();
    }

    /**
     * 传入的值为"true"、"nativejava"或"bean"时，返回true
     *
     * @param generic
     * @return
     */
    public static boolean isGeneric(String generic) {
        return generic != null
                && !"".equals(generic)
                && (Constants.GENERIC_SERIALIZATION_DEFAULT.equalsIgnoreCase(generic)  /* Normal generalization cal */
                || Constants.GENERIC_SERIALIZATION_NATIVE_JAVA.equalsIgnoreCase(generic) /* Streaming generalization call supporting jdk serialization */
                || Constants.GENERIC_SERIALIZATION_BEAN.equalsIgnoreCase(generic));
    }

    /**
     * 是否为默认的泛化调用
     *
     * @param generic
     * @return
     */
    public static boolean isDefaultGenericSerialization(String generic) {
        return isGeneric(generic)
                && Constants.GENERIC_SERIALIZATION_DEFAULT.equalsIgnoreCase(generic);
    }

    /**
     * 是否为支持jdk序列化的流泛化调用
     *
     * @param generic
     * @return
     */
    public static boolean isJavaGenericSerialization(String generic) {
        return isGeneric(generic)
                && Constants.GENERIC_SERIALIZATION_NATIVE_JAVA.equalsIgnoreCase(generic);
    }

    /**
     * 是否为bean方式的泛化调用
     *
     * @param generic
     * @return
     */
    public static boolean isBeanGenericSerialization(String generic) {
        return isGeneric(generic) && Constants.GENERIC_SERIALIZATION_BEAN.equals(generic);
    }
}
