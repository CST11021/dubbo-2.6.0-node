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
 *
 * 用于封装RPC接口调用的返回值
 *
 * RPC invoke result. (API, Prototype, NonThreadSafe)
 *
 * @serial Don't change the class name and package name.
 * @see com.alibaba.dubbo.rpc.Invoker#invoke(Invocation)
 * @see com.alibaba.dubbo.rpc.RpcResult
 */
public interface Result {

    /**
     * 获取调用的返回值
     *
     * @return result. 如果没有返回值，则返回null
     */
    Object getValue();

    /**
     * 如果调用服务异常，则通过该方法返回异常信息
     *
     * @return exception. if no exception return null.
     */
    Throwable getException();

    /**
     * 判断调用的RPC服务是否异常
     *
     * @return has exception.
     */
    boolean hasException();

    /**
     * 有异常的话抛出异常，没有异常的话返回执行结果值，实现逻辑如下
     * <p>
     * <code>
     * if (hasException()) {
     *      throw getException();
     * } else {
     *      return getValue();
     * }
     * </code>
     *
     * @return result.
     * @throws if has exception throw it.
     */
    Object recreate() throws Throwable;

    /**
     * @see com.alibaba.dubbo.rpc.Result#getValue()
     * @deprecated Replace to getValue()
     */
    @Deprecated
    Object getResult();


    // ----------------------------------------------------------------
    // 用于获取此次调用一些额外信息，比如dubbo版本，分组，设置的超时信息等
    // ----------------------------------------------------------------

    Map<String, String> getAttachments();
    String getAttachment(String key);
    String getAttachment(String key, String defaultValue);

}