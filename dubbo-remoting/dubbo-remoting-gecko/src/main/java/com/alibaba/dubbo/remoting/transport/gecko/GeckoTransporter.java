package com.alibaba.dubbo.remoting.transport.gecko;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.*;

/**
 * @Author: wanghz
 * @Date: 2020/12/19 5:28 下午
 */
public class GeckoTransporter implements Transporter {

    @Override
    public Server bind(URL url, ChannelHandler handler) throws RemotingException {
        return new GeckoServer(url, handler);
    }

    @Override
    public Client connect(URL url, ChannelHandler handler) throws RemotingException {
        return null;
    }

}
