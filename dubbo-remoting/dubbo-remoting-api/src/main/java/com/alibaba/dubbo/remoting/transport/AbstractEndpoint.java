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
package com.alibaba.dubbo.remoting.transport;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.Resetable;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Codec;
import com.alibaba.dubbo.remoting.Codec2;
import com.alibaba.dubbo.remoting.transport.codec.CodecAdapter;

/**
 * AbstractEndpoint 扩展自AbstractPeer，增加了编解码组件和从URL获取超时信息，该类有两个抽象子类，用于扩展：
 * {@link AbstractClient} 和 {@link AbstractServer}
 */
public abstract class AbstractEndpoint extends AbstractPeer implements Resetable {

    private static final Logger logger = LoggerFactory.getLogger(AbstractEndpoint.class);
    /** 对通道数据进行编解码 */
    private Codec2 codec;
    /** 客户端（通道）建立连接时的超时时间（timeout是给Mina、Grizzly和netty4的客户端用的） */
    private int timeout;
    /** 客户端（通道）建立连接时的超时时间（connectTimeout是给netty的客户端用）*/
    private int connectTimeout;

    public AbstractEndpoint(URL url, ChannelHandler handler) {
        super(url, handler);
        // 据SPI机制获取一个编解码器
        this.codec = getChannelCodec(url);
        this.timeout = url.getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        this.connectTimeout = url.getPositiveParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT);
    }


    /** 从url中提取timeout、connectTimeout和codec信息 */
    public void reset(URL url) {
        if (isClosed()) {
            throw new IllegalStateException("Failed to reset parameters " + url + ", cause: Channel closed. channel: " + getLocalAddress());
        }

        // 获取通信的超时时间
        try {
            if (url.hasParameter(Constants.TIMEOUT_KEY)) {
                int t = url.getParameter(Constants.TIMEOUT_KEY, 0);
                if (t > 0) {
                    this.timeout = t;
                }
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }

        // 获取连接的超时时间
        try {
            if (url.hasParameter(Constants.CONNECT_TIMEOUT_KEY)) {
                int t = url.getParameter(Constants.CONNECT_TIMEOUT_KEY, 0);
                if (t > 0) {
                    this.connectTimeout = t;
                }
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }

        // 获取编解码实现
        try {
            if (url.hasParameter(Constants.CODEC_KEY)) {
                this.codec = getChannelCodec(url);
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }

    @Deprecated
    public void reset(com.alibaba.dubbo.common.Parameters parameters) {
        reset(getUrl().addParameters(parameters.getParameters()));
    }

    /**
     * 获取一个编解码器的实例
     *
     * @return
     */
    protected Codec2 getCodec() {
        return codec;
    }

    protected int getTimeout() {
        return timeout;
    }

    protected int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 根据SPI机制获取一个编解码器
     *
     * @param url
     * @return
     */
    protected static Codec2 getChannelCodec(URL url) {
        String codecName = url.getParameter(Constants.CODEC_KEY, "telnet");
        if (ExtensionLoader.getExtensionLoader(Codec2.class).hasExtension(codecName)) {
            return ExtensionLoader.getExtensionLoader(Codec2.class).getExtension(codecName);
        } else {
            // 这里返回的是CodecAdapter，内部的编解码逻辑还是通过Codec来实现的
            return new CodecAdapter(ExtensionLoader.getExtensionLoader(Codec.class).getExtension(codecName));
        }
    }
}