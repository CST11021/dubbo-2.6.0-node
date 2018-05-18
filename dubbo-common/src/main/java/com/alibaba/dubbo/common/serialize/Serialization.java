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
package com.alibaba.dubbo.common.serialize;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Dubbo序列化的顶级接口，实现类如下：
 * NativeJavaSerialization :  Java自带的序列化实现
 * CompactedJavaSerialization : 压缩java序列化，主要是在原生java序列化基础上，实现了自己定义的类描写叙述符写入和读取
 *                              写Object类型的类描写叙述符仅仅写入类名称，而不是类的完整信息。这样有非常多Object类型的情况下能够降低序列化后的size
 * JavaSerialization : 仅仅是对原生java序列化和压缩java序列化的封装
 * JsonSerialization : 自己实现的JSON序列化实现
 * FastJsonSerialization : 使用阿里的FastJson实现的序列化
 * Hessian2Serialization：使用Hessian2的IO机制实现的序列化(默认实现)
 * DubboSerialization：Dubbo自己定义的序列化实现
 *
 *
 * 使用示例：
Serialization serialization = new Hessian2Serialization();
URL url = new URL("protocl", "1.1.1.1", 1234);
ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
objectOutput.writeObject(data);
objectOutput.flushBuffer();

ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
byteArrayOutputStream.toByteArray());
ObjectInput deserialize = serialization.deserialize(url, byteArrayInputStream);

T data = (T) deserialize.readObject();
 *
 *
 *
 * Serialization. (SPI, Singleton, ThreadSafe)
 */
@SPI("hessian2")
public interface Serialization {

    /**
     * 表示序列化类型的ID，例如：
     * DubboSerialization：          id=1，type="x-application/dubbo"
     * Hessian2Serialization：       id=2，type=""x-application/hessian2"
     * JavaSerialization：           id=3，type=""x-application/java"
     * CompactedJavaSerialization：  id=4，type=""x-application/compactedjava"
     * JsonSerialization：           id=5，type=""text/json"
     * FastJsonSerialization：       id=6，type=""text/json"
     * NativeJavaSerialization：     id=7，type=""x-application/nativejava"
     * KryoSerialization：           id=8，type=""x-application/kryo"
     * FstSerialization：            id=9，type=""x-application/fst"
     *
     * @return content type id
     */
    byte getContentTypeId();

    /**
     * 表示序列化的类型
     *
     * @return content type
     */
    String getContentType();

    /**
     * 创建一个ObjectOutput对象用于，序列化对象
     *
     * @param url
     * @param output
     * @return serializer
     * @throws IOException
     */
    @Adaptive
    ObjectOutput serialize(URL url, OutputStream output) throws IOException;

    /**
     * 创建一个ObjectInput对象，用于反序列化
     *
     * @param url
     * @param input
     * @return deserializer
     * @throws IOException
     */
    @Adaptive
    ObjectInput deserialize(URL url, InputStream input) throws IOException;

}