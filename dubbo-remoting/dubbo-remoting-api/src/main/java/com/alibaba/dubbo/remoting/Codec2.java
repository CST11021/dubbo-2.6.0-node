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
package com.alibaba.dubbo.remoting;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;

import java.io.IOException;

/**
 * Dubbo的远程调用需要对传输的数据进行编码解码，dubbo的Codec2接口定义了编码解码规范，与废弃的接口Codec相比，Codec2没有依赖jdk
 * 的输入输出流， 以dubbo的ChannelBuffer为核心便于更好的整合
 */
@SPI
public interface Codec2 {

    /**
     * 将message进行编码并写入buffer
     *
     * @param channel
     * @param buffer
     * @param message
     * @throws IOException
     */
    @Adaptive({Constants.CODEC_KEY})
    void encode(Channel channel, ChannelBuffer buffer, Object message) throws IOException;

    /**
     * 将buffer中的数据进行解码并返回
     *
     * @param channel
     * @param buffer
     * @return
     * @throws IOException
     */
    @Adaptive({Constants.CODEC_KEY})
    Object decode(Channel channel, ChannelBuffer buffer) throws IOException;


    enum DecodeResult {
        NEED_MORE_INPUT, SKIP_SOME_INPUT
    }

}

