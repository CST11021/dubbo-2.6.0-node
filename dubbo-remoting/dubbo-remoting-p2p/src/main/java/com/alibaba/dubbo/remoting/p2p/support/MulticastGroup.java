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
package com.alibaba.dubbo.remoting.p2p.support;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.p2p.Peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

/**
 * Socket广播实现
 */
public class MulticastGroup extends AbstractGroup {

    private static final String JOIN = "join";

    private static final String LEAVE = "leave";

    /** 表示广播的地址 */
    private InetAddress mutilcastAddress;

    private MulticastSocket mutilcastSocket;

    public MulticastGroup(URL url) {
        super(url);
        // ip必须是有效的广播地址
        if (!isMulticastAddress(url.getHost())) {
            throw new IllegalArgumentException("Invalid multicast address " + url.getHost() + ", scope: 224.0.0.0 - 239.255.255.255");
        }

        try {
            mutilcastAddress = InetAddress.getByName(url.getHost());
            mutilcastSocket = new MulticastSocket(url.getPort());
            mutilcastSocket.setLoopbackMode(false);
            mutilcastSocket.joinGroup(mutilcastAddress);

            // 用于监听服务join和leave线程
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    byte[] buf = new byte[1024];
                    DatagramPacket recv = new DatagramPacket(buf, buf.length);
                    while (true) {
                        try {
                            mutilcastSocket.receive(recv);
                            MulticastGroup.this.receive(new String(recv.getData()).trim(), (InetSocketAddress) recv.getSocketAddress());
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }, "MulticastGroupReceiver");
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * ip必须是有效的广播地址
     *
     * @param ip
     * @return
     */
    private static boolean isMulticastAddress(String ip) {
        int i = ip.indexOf('.');
        if (i > 0) {
            String prefix = ip.substring(0, i);
            if (StringUtils.isInteger(prefix)) {
                int p = Integer.parseInt(prefix);
                return p >= 224 && p <= 239;
            }
        }
        return false;
    }

    /**
     * 广播一条消息
     *
     * @param msg
     * @throws RemotingException
     */
    private void send(String msg) throws RemotingException {
        DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), mutilcastAddress, mutilcastSocket.getLocalPort());
        try {
            mutilcastSocket.send(hi);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * 接收消息：
     * 如果是JOIN类型的消息，则创建一个连接对象，加入该服务组
     * 如果是LEAVE类型的消息，则断开链接对象，从该服务组移除
     *
     * @param msg
     * @param remoteAddress
     * @throws RemotingException
     */
    private void receive(String msg, InetSocketAddress remoteAddress) throws RemotingException {
        if (msg.startsWith(JOIN)) {
            String url = msg.substring(JOIN.length()).trim();
            connect(URL.valueOf(url));
        } else if (msg.startsWith(LEAVE)) {
            String url = msg.substring(LEAVE.length()).trim();
            disconnect(URL.valueOf(url));
        }
    }

    @Override
    public Peer join(URL url, ChannelHandler handler) throws RemotingException {
        Peer peer = super.join(url, handler);
        send(JOIN + " " + url.toFullString());
        return peer;
    }

    @Override
    public void leave(URL url) throws RemotingException {
        super.leave(url);
        send(LEAVE + " " + url.toFullString());
    }

}