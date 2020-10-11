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
package com.alibaba.dubbo.remoting.transport.codec;

import com.alibaba.dubbo.common.serialize.Cleanable;
import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;
import com.alibaba.dubbo.remoting.buffer.ChannelBufferInputStream;
import com.alibaba.dubbo.remoting.buffer.ChannelBufferOutputStream;
import com.alibaba.dubbo.remoting.transport.AbstractCodec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * TransportCodec：数据传输层的编解码器
 */
public class TransportCodec extends AbstractCodec {

    public void encode(Channel channel, ChannelBuffer buffer, Object message) throws IOException {
        // 1、构建ChannelBufferOutputStream，是的buffer具有jdk OutputStream的api操作功能，因为序列化工具都是基于jdkAPI的
        OutputStream output = new ChannelBufferOutputStream(buffer);
        // 2、getSerialization(channel) 通过Dubbo的SPI扩展机制得到具体的序列化工具
        ObjectOutput objectOutput = getSerialization(channel).serialize(channel.getUrl(), output);
        // 3、将数据序列化后写入传输通道
        encodeData(channel, objectOutput, message);
        objectOutput.flushBuffer();
        if (objectOutput instanceof Cleanable) {
            ((Cleanable) objectOutput).cleanup();
        }
    }

    public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {
        // 1、构建ChannelBufferInputStream是的序列化工具能够通过jdk的api读取channelBuffer数据的功能
        InputStream input = new ChannelBufferInputStream(buffer);
        // 2. 通过Dubbo的SPI扩展机制得到具体的序列化实现进行反序列实现
        ObjectInput objectInput = getSerialization(channel).deserialize(channel.getUrl(), input);
        // 3. decodeData这里只是获取反序列化对象
        Object object = decodeData(channel, objectInput);
        if (objectInput instanceof Cleanable) {
            ((Cleanable) objectInput).cleanup();
        }
        return object;
    }

    protected void encodeData(Channel channel, ObjectOutput output, Object message) throws IOException {
        encodeData(output, message);
    }

    protected Object decodeData(Channel channel, ObjectInput input) throws IOException {
        return decodeData(input);
    }

    protected void encodeData(ObjectOutput output, Object message) throws IOException {
        output.writeObject(message);
    }

    protected Object decodeData(ObjectInput input) throws IOException {
        try {
            return input.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("ClassNotFoundException: " + StringUtils.toString(e));
        }
    }
}