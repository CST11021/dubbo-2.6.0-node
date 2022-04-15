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
import com.alibaba.dubbo.common.utils.IOUtils;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.p2p.Peer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 通过文件配置的方式，维护一组服务节点信息，添加一个服务和移除服务都会修改配置文件
 */
public class FileGroup extends AbstractGroup {

    /** 保存服务节点的配置文件 */
    private final File file;
    /** 检查配置是否被修改的定时任务 */
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3, new NamedThreadFactory("FileGroupModifiedChecker", true));
    /** 重连定时器一次检查连接是否可用，不可用时无限重连 */
    private final ScheduledFuture<?> checkModifiedFuture;
    /** 表示最后一次修改配置文件的时间戳 */
    private volatile long last;

    public FileGroup(URL url) {
        super(url);
        // 从url获取配置文件的绝对路径地址，然后创建一个文件
        String path = url.getAbsolutePath();
        file = new File(path);

        // 创建一个定时任务，检查配置是否被修改
        checkModifiedFuture = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                // 检查配置文件是否被修改，如果配置文件被修改，则获取配置文件的服务节点信息，并遍历每个服务节点创建连接
                try {
                    check();
                } catch (Throwable t) {
                    logger.error("Unexpected error occur at reconnect, cause: " + t.getMessage(), t);
                }
            }
        }, 2000, 2000, TimeUnit.MILLISECONDS);
    }

    public void close() {
        super.close();
        try {
            if (!checkModifiedFuture.isCancelled()) {
                checkModifiedFuture.cancel(true);
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }

    /**
     * 检查配置文件是否被修改，如果配置文件被修改，则获取配置文件的服务节点信息，并遍历每个服务节点创建连接
     *
     * @throws RemotingException
     */
    private void check() throws RemotingException {
        long modified = file.lastModified();
        if (modified > last) {
            last = modified;
            changed();
        }
    }

    /**
     * 文件配置的服务节点信息发生变化时会调用方法，并遍历每个服务节点创建连接
     *
     * @throws RemotingException
     */
    private void changed() throws RemotingException {
        try {
            String[] lines = IOUtils.readLines(file);
            for (String line : lines) {
                connect(URL.valueOf(line));
            }
        } catch (IOException e) {
            throw new RemotingException(new InetSocketAddress(NetUtils.getLocalHost(), 0), getUrl().toInetSocketAddress(), e.getMessage(), e);
        }
    }

    /**
     * 追加一个服务节点到配置文件
     *
     * @param url
     * @param handler
     * @return
     * @throws RemotingException
     */
    public Peer join(URL url, ChannelHandler handler) throws RemotingException {
        Peer peer = super.join(url, handler);
        try {
            String full = url.toFullString();
            String[] lines = IOUtils.readLines(file);
            for (String line : lines) {
                if (full.equals(line)) {
                    return peer;
                }
            }
            IOUtils.appendLines(file, new String[]{full});
        } catch (IOException e) {
            throw new RemotingException(new InetSocketAddress(NetUtils.getLocalHost(), 0), getUrl().toInetSocketAddress(), e.getMessage(), e);
        }
        return peer;
    }

    /**
     * 将指定服务的节点信息从文件中移除
     *
     * @param url
     * @throws RemotingException
     */
    @Override
    public void leave(URL url) throws RemotingException {
        super.leave(url);
        try {
            String full = url.toFullString();
            String[] lines = IOUtils.readLines(file);

            List<String> saves = new ArrayList<String>();
            for (String line : lines) {
                if (full.equals(line)) {
                    return;
                }
                saves.add(line);
            }
            IOUtils.appendLines(file, saves.toArray(new String[0]));
        } catch (IOException e) {
            throw new RemotingException(new InetSocketAddress(NetUtils.getLocalHost(), 0), getUrl().toInetSocketAddress(), e.getMessage(), e);
        }
    }

}