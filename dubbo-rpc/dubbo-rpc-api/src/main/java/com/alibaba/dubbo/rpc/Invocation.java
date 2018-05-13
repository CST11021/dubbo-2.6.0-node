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
package com.alibaba.dubbo.rpc;

import java.util.Map;

/**
 * Invocation. (API, Prototype, NonThreadSafe)
 *
 * @serial Don't change the class name and package name.
 * @see com.alibaba.dubbo.rpc.Invoker#invoke(Invocation)
 * @see com.alibaba.dubbo.rpc.RpcInvocation
 */
public interface Invocation {

    /**
     * 获取调用的方法名
     *
     * @return method name.
     * @serial
     */
    String getMethodName();

    /**
     * 获取调用目标方法的入参类型
     *
     * @return parameter types.
     * @serial
     */
    Class<?>[] getParameterTypes();

    /**
     * 获取调用目标方法的入参
     *
     * @return arguments.
     * @serial
     */
    Object[] getArguments();

    /**
     * 返回当前调用的上下文，即目标对象
     *
     * @return invoker.
     * @transient
     */
    Invoker<?> getInvoker();

    // ----------------------------------------------------------------
    // 用于获取此次调用一些额外信息，比如dubbo版本，分组，设置的超时信息等
    // ----------------------------------------------------------------

    Map<String, String> getAttachments();
    String getAttachment(String key);
    String getAttachment(String key, String defaultValue);



}