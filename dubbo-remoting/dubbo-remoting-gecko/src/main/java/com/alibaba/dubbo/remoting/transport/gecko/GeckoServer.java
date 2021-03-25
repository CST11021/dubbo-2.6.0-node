package com.alibaba.dubbo.remoting.transport.gecko;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.ExecutorUtil;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.AbstractServer;
import com.alibaba.dubbo.remoting.transport.dispatcher.ChannelHandlers;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.service.*;
import com.taobao.gecko.service.config.ServerConfig;

import java.net.InetSocketAddress;
import java.util.*;

/**
 * @Author: wanghz
 * @Date: 2020/12/9 9:30 上午
 */
public class GeckoServer extends AbstractServer {

    private Map<InetSocketAddress, Channel> remoteAddress2Channel = new HashMap();

    private RemotingServer remotingServer;

    public GeckoServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, ChannelHandlers.wrap(handler, ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME)));
    }

    /**
     * 启动一个Gecko服务，并开始监听客户端请求
     *
     * @throws Throwable
     */
    @Override
    protected void doOpen() throws Throwable {
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setWireFormatType(new GeckoWireFormatType());
        serverConfig.setLocalInetSocketAddress(getBindAddress());

        remotingServer = RemotingFactory.newRemotingServer(serverConfig);
        remotingServer.registerProcessor(RequestCommand.class, new GeckoRequestProcessor(getUrl(), this));
        remotingServer.start();
    }

    @Override
    public boolean isBound() {
        if (remotingServer.isStarted()) {
            InetSocketAddress address = remotingServer.getInetSocketAddress();
            if (address != null && address.equals(getBindAddress())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void connected(Channel ch) throws RemotingException {
        super.connected(ch);
        InetSocketAddress remoteAddress = ch.getRemoteAddress();
        remoteAddress2Channel.put(remoteAddress, ch);
    }

    @Override
    public Collection<Channel> getChannels() {
        return remoteAddress2Channel.values();
    }

    @Override
    public Channel getChannel(InetSocketAddress remoteAddress) {
        return remoteAddress2Channel.get(remoteAddress);
    }

    @Override
    protected void doClose() throws Throwable {
        if (remotingServer != null) {
            remotingServer.stop();
        }
    }



}
