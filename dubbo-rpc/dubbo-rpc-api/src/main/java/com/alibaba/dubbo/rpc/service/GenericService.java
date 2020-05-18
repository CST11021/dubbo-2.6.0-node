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
package com.alibaba.dubbo.rpc.service;

/**
 * 表示一个泛化服务类型
 *
 * @export
 */
public interface GenericService {

    /**
     * Generic invocation
     *
     * @param method         方法名称，例如findPerson。如果有覆盖的方法，则需要参数信息，例如findPerson（java.lang.String）
     * @param parameterTypes 参数类型
     * @param args           参数
     * @return 方法调用返回值
     * @throws Throwable potential exception thrown from the invocation
     */
    Object $invoke(String method, String[] parameterTypes, Object[] args) throws GenericException;

}