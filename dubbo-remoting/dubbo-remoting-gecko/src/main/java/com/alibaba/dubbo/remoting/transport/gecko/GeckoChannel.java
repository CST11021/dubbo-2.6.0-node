package com.alibaba.dubbo.remoting.transport.gecko;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.transport.AbstractChannel;
import com.taobao.gecko.service.Connection;

import java.net.InetSocketAddress;

/**
 * @Author: wanghz
 * @Date: 2021/1/5 10:58 上午
 */
public class GeckoChannel extends AbstractChannel {

    Connection connection = null;

    public GeckoChannel(Connection connection, URL url, ChannelHandler handler) {
        super(url, handler);

    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean hasAttribute(String key) {
        return false;
    }

    @Override
    public Object getAttribute(String key) {
        return null;
    }

    @Override
    public void setAttribute(String key, Object value) {

    }

    @Override
    public void removeAttribute(String key) {

    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

}
