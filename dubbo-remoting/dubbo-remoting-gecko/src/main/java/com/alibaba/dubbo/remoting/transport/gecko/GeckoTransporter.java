package com.alibaba.dubbo.remoting.transport.gecko;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.*;

/**
 * @Author: wanghz
 * @Date: 2020/12/19 5:28 下午
 */
public class GeckoTransporter implements Transporter {

    /**
     * 创建一个服务监听，开始监听客户端请求
     *
     * @param url       服务地址、端口及网络通讯层的相关配置
     * @param handler   Channel事件相关的监听
     * @return
     * @throws RemotingException
     */
    @Override
    public Server bind(URL url, ChannelHandler handler) throws RemotingException {
        return new GeckoServer(url, handler);
    }

    /**
     * 创建一个客户端对象
     *
     * @param url       连接的服务地址及端口，以及网络通讯层的相关配置
     * @param handler   Channel事件相关的监听
     * @return
     * @throws RemotingException
     */
    @Override
    public Client connect(URL url, ChannelHandler handler) throws RemotingException {
        return null;
    }

}
