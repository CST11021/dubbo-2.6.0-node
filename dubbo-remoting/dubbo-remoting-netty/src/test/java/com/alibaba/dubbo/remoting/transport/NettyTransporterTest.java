package com.alibaba.dubbo.remoting.transport;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.remoting.*;
import com.alibaba.dubbo.remoting.transport.netty.NettyServer;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * @author whz
 * @version : NettyTransporterTest.java, v 0.1 2018-05-21 14:19 whz Exp $$
 */
public class NettyTransporterTest {

    private static final int port = 9999;
    private static Transporter transporter = ExtensionLoader.getExtensionLoader(Transporter.class).getAdaptiveExtension();

    @Test
    public void serverTest() throws RemotingException, IOException {
        transporter.bind(URL.valueOf("netty://localhost").setPort(port), new TestChannelHandler());
        System.in.read();
    }

    @Test
    public void clientTest() throws RemotingException {
        Client client = transporter.connect(URL.valueOf("netty://localhost").setPort(port), new TestChannelHandler());
        client.send("hello");
    }

    static class TestChannelHandler implements ChannelHandler {

        @Override
        public void connected(Channel channel) throws RemotingException {
            System.out.println("connected");
        }

        @Override
        public void disconnected(Channel channel) throws RemotingException {
            System.out.println("disconnected");
        }

        @Override
        public void sent(Channel channel, Object message) throws RemotingException {
            System.out.println("sent:" + message);
        }

        @Override
        public void received(Channel channel, Object message) throws RemotingException {
            System.out.println("received:" + message);
        }

        @Override
        public void caught(Channel channel, Throwable exception) throws RemotingException {
            System.out.println("caught:" + exception.getMessage());
        }
    }
}
