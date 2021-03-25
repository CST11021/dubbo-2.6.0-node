package com.alibaba.dubbo.remoting.transport.gecko;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.RequestProcessor;

import java.util.concurrent.ThreadPoolExecutor;

public class GeckoRequestProcessor implements RequestProcessor {

    private final URL url;

    /** 通道事件监听处理器 */
    private final ChannelHandler handler;

    public GeckoRequestProcessor(URL url, ChannelHandler handler) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        this.url = url;
        this.handler = handler;
    }

    @Override
    public void handleRequest(RequestCommand requestCommand, Connection connection) {

    }

    @Override
    public ThreadPoolExecutor getExecutor() {
        return null;
    }

}

