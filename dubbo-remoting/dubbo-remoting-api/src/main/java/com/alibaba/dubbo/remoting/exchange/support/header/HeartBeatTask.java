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

package com.alibaba.dubbo.remoting.exchange.support.header;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.exchange.Request;

import java.util.Collection;

/**
 * 心跳检测任务
 */
final class HeartBeatTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(HeartBeatTask.class);

    /** 用于获取通道的策略接口 */
    private ChannelProvider channelProvider;

    private int heartbeat;

    private int heartbeatTimeout;

    HeartBeatTask(ChannelProvider provider, int heartbeat, int heartbeatTimeout) {
        this.channelProvider = provider;
        this.heartbeat = heartbeat;
        this.heartbeatTimeout = heartbeatTimeout;
    }

    /**
     * dubbo心跳时间heartbeat默认是60s，超过heartbeat时间没有收到消息，就发送心跳消息(provider，consumer一样)，
     * 如果连着3次(heartbeatTimeout为heartbeat*3)没有收到心跳响应，provider会关闭channel，而consumer会进行重连；
     * 不论是provider还是consumer的心跳检测都是通过启动定时任务的方式实现；
     */
    public void run() {
        try {
            long now = System.currentTimeMillis();

            // 遍历所有的channel，检测心跳间隔,如果超过心跳间隔没有读或写，则发送需要回复的心跳消息，最有判断是否心跳超时(heartbeatTimeout)，
            // 如果超时，provider关闭channel，consumer进行重连
            for (Channel channel : channelProvider.getChannels()) {
                if (channel.isClosed()) {
                    continue;
                }

                try {
                    Long lastRead = (Long) channel.getAttribute(HeaderExchangeHandler.KEY_READ_TIMESTAMP);
                    Long lastWrite = (Long) channel.getAttribute(HeaderExchangeHandler.KEY_WRITE_TIMESTAMP);

                    // 最后一次读操作时间或者写操作时间超过了heartbeat
                    if ((lastRead != null && now - lastRead > heartbeat)
                            || (lastWrite != null && now - lastWrite > heartbeat)) {

                        Request req = new Request();
                        req.setVersion("2.0.0");
                        req.setTwoWay(true);
                        req.setEvent(Request.HEARTBEAT_EVENT);
                        channel.send(req);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Send heartbeat to remote channel " + channel.getRemoteAddress()
                                    + ", cause: The channel has no data-transmission exceeds a heartbeat period: " + heartbeat + "ms");
                        }
                    }

                    // 最后读的时间，超过心跳超时时间，客户端会重连服务端，服务端会关闭客户端连接
                    if (lastRead != null && now - lastRead > heartbeatTimeout) {
                        logger.warn("Close channel " + channel + ", because heartbeat read idle time out: " + heartbeatTimeout + "ms");
                        if (channel instanceof Client) {
                            try {
                                ((Client) channel).reconnect();
                            } catch (Exception e) {
                                //do nothing
                            }
                        } else {
                            channel.close();
                        }
                    }

                } catch (Throwable t) {
                    logger.warn("Exception when heartbeat to remote channel " + channel.getRemoteAddress(), t);
                }
            }
        } catch (Throwable t) {
            logger.warn("Unhandled exception when heartbeat, cause: " + t.getMessage(), t);
        }
    }


    interface ChannelProvider {
        Collection<Channel> getChannels();
    }

}

