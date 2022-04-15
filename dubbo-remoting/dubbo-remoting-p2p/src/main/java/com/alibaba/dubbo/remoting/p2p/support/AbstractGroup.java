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
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Server;
import com.alibaba.dubbo.remoting.Transporters;
import com.alibaba.dubbo.remoting.p2p.Group;
import com.alibaba.dubbo.remoting.p2p.Peer;
import com.alibaba.dubbo.remoting.transport.ChannelHandlerDispatcher;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbstractGroup
 */
public abstract class AbstractGroup implements Group {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractGroup.class);

    /** 表示维护一组服务器集合的配置，可能是一个配置文件，或者是网络组播对象 */
    protected final URL url;

    /** 表示该组里维护的Server集合 */
    protected final Map<URL, Server> servers = new ConcurrentHashMap<URL, Server>();

    /** 表示连接了每个Server的客户端对象 */
    protected final Map<URL, Client> clients = new ConcurrentHashMap<URL, Client>();

    protected final ChannelHandlerDispatcher dispatcher = new ChannelHandlerDispatcher();

    public AbstractGroup(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        this.url = url;
    }

    /**
     * 获取该服务组的信息
     *
     * @return
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Group关闭：关闭组内所有的服务和连接实例
     */
    public void close() {
        for (URL url : new ArrayList<URL>(servers.keySet())) {
            try {
                leave(url);
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        }

        for (URL url : new ArrayList<URL>(clients.keySet())) {
            try {
                disconnect(url);
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        }
    }

    /**
     * 将一个服务节点添加组，并返回一个代表服务组的Peer对象
     *
     * @param url
     * @param handler
     * @return
     * @throws RemotingException
     */
    public Peer join(URL url, ChannelHandler handler) throws RemotingException {
        Server server = servers.get(url);
        // TODO exist concurrent gap
        if (server == null) {
            server = Transporters.bind(url, handler);
            servers.put(url, server);
            dispatcher.addChannelHandler(handler);
        }

        return new ServerPeer(server, clients, this);
    }

    /**
     * 关闭所有的服务
     *
     * @param url
     * @throws RemotingException
     */
    public void leave(URL url) throws RemotingException {
        Server server = servers.remove(url);
        if (server != null) {
            server.close();
        }
    }

    /**
     * 根据url，创建一个连接服务器的Client对象
     *
     * @param url
     * @return
     * @throws RemotingException
     */
    protected Client connect(URL url) throws RemotingException {
        if (servers.containsKey(url)) {
            return null;
        }

        Client client = clients.get(url);
        // TODO exist concurrent gap
        if (client == null) {
            client = Transporters.connect(url, dispatcher);
            clients.put(url, client);
        }
        return client;
    }

    protected void disconnect(URL url) throws RemotingException {
        Client client = clients.remove(url);
        if (client != null) {
            client.close();
        }
    }

}