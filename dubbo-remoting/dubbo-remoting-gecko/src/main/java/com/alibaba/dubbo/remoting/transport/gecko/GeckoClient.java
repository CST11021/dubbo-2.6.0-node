package com.alibaba.dubbo.remoting.transport.gecko;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.AbstractClient;
import com.taobao.gecko.service.RemotingClient;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.config.ClientConfig;

/**
 * @Author: wanghz
 * @Date: 2021/1/5 10:06 上午
 */
public class GeckoClient extends AbstractClient {

    private String uri;

    private RemotingClient remotingClient;

    public GeckoClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
        this.uri = "gecko://" + getRemoteAddress().getHostName() + getRemoteAddress().getPort();
    }

    /**
     * 使用Gecko框架创建一个客户端，注意，这里仅仅只是创建Gecko的客户端对象，并不发起连接服务端请求，连接服务端请求的动作在{@link #doConnect()}方法中实现
     *
     * @throws Throwable
     */
    @Override
    protected void doOpen() throws Throwable {
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setWireFormatType(new GeckoWireFormatType());

        RemotingClient remotingClient = RemotingFactory.newRemotingClient(clientConfig);
        remotingClient.start();
    }

    @Override
    protected void doClose() throws Throwable {
        remotingClient.close(uri, false);
    }

    @Override
    protected void doConnect() throws Throwable {
        remotingClient.connect(uri);
        remotingClient.awaitReadyInterrupt(uri);
    }

    @Override
    protected void doDisConnect() throws Throwable {

    }

    @Override
    protected Channel getChannel() {
        return null;
    }

}
