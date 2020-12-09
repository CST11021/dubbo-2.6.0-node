package com.alibaba.dubbo.remoting.transport.gecko;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.ExecutorUtil;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.AbstractServer;
import com.alibaba.dubbo.remoting.transport.dispatcher.ChannelHandlers;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.RemotingServer;
import com.taobao.gecko.service.config.ServerConfig;

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * @Author: wanghz
 * @Date: 2020/12/9 9:30 上午
 */
public class GeckoServer extends AbstractServer {

    private RemotingServer remotingServer;

    public GeckoServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
    }

    @Override
    protected void doOpen() throws Throwable {
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setWireFormatType(new GeckoWireFormatType());
        serverConfig.setLocalInetSocketAddress(getBindAddress());

        remotingServer = RemotingFactory.newRemotingServer(serverConfig);
    }

    @Override
    public boolean isBound() {
        return remotingServer.isStarted();
    }

    @Override
    public Collection<Channel> getChannels() {
        // remotingServer.getRemotingContext().
        return null;
    }

    @Override
    public Channel getChannel(InetSocketAddress remoteAddress) {
        return null;
    }



    @Override
    protected void doClose() throws Throwable {

    }

}
