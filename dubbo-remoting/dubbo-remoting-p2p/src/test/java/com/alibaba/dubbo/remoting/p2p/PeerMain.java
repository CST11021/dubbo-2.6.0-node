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
package com.alibaba.dubbo.remoting.p2p;

import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.ChannelHandlerAdapter;

import java.util.Collection;

/**
 * PeerMain
 */
public class PeerMain {

    public static void main(String[] args) throws Throwable {
        String groupURL = "multicast://224.5.6.7:9911"; // 组地址，支持组播和文件两组，可扩展
        final String peerURL = "dubbo://0.0.0.0:" + (((int) (Math.random() * 10000)) + 20000); // 跨网络的本机服务器地址

        // Join the group and get the peer reference
        Peer peer = Networkers.join(groupURL, peerURL, new ChannelHandlerAdapter() {
            @Override
            public void received(Channel channel, Object message) throws RemotingException {
                System.out.println("Received: " + message + " in " + peerURL);
            }
        });

        // Sending messages to other peers in the network
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            // Access to channels with all other peers, this list changes dynamically
            Collection<Channel> channels = peer.getChannels();
            if (channels != null && channels.size() > 0) {
                for (Channel channel : channels) {
                    // Sending messages to a specified peer
                    channel.send("(" + i + ") " + peerURL);
                }
            }
            Thread.sleep(1000);
        }

        // leave the network
        peer.leave();
    }

}