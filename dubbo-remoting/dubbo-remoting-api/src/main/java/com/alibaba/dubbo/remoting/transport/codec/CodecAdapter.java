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

import com.alibaba.dubbo.common.io.UnsafeByteArrayInputStream;
import com.alibaba.dubbo.common.io.UnsafeByteArrayOutputStream;
import com.alibaba.dubbo.common.utils.Assert;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Codec;
import com.alibaba.dubbo.remoting.Codec2;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;

import java.io.IOException;

public class CodecAdapter implements Codec2 {

    /** 编解码器 */
    private Codec codec;

    public CodecAdapter(Codec codec) {
        Assert.notNull(codec, "codec == null");
        this.codec = codec;
    }

    /**
     * 将message进行编码并写入buffer
     *
     * @param channel
     * @param buffer
     * @param message
     * @throws IOException
     */
    public void encode(Channel channel, ChannelBuffer buffer, Object message) throws IOException {
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream(1024);
        // 将message进行编码，并写入输出流os中
        codec.encode(channel, os, message);
        // 将os输出流程中的字节写入buffer
        buffer.writeBytes(os.toByteArray());
    }

    /**
     * 将buffer中的数据进行解码并返回
     *
     * @param channel
     * @param buffer
     * @return
     * @throws IOException
     */
    public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {
        byte[] bytes = new byte[buffer.readableBytes()];
        int savedReaderIndex = buffer.readerIndex();
        buffer.readBytes(bytes);
        UnsafeByteArrayInputStream is = new UnsafeByteArrayInputStream(bytes);
        Object result = codec.decode(channel, is);
        buffer.readerIndex(savedReaderIndex + is.position());
        return result == Codec.NEED_MORE_INPUT ? DecodeResult.NEED_MORE_INPUT : result;
    }

    /**
     * 获取编解码器
     *
     * @return
     */
    public Codec getCodec() {
        return codec;
    }
}
