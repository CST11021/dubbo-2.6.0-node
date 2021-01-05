package com.alibaba.dubbo.remoting.transport.gecko;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.AbstractClient;

/**
 * @Author: wanghz
 * @Date: 2021/1/5 10:06 上午
 */
public class GeckoClient extends AbstractClient {


    public GeckoClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
    }

    @Override
    protected void doOpen() throws Throwable {

    }

    @Override
    protected void doClose() throws Throwable {

    }

    @Override
    protected void doConnect() throws Throwable {

    }

    @Override
    protected void doDisConnect() throws Throwable {

    }

    @Override
    protected Channel getChannel() {
        return null;
    }
}
