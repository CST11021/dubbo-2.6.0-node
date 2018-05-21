package com.alibaba.dubbo.remoting.transport;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.*;
import com.alibaba.dubbo.remoting.transport.netty.Hello;
import com.alibaba.dubbo.remoting.transport.netty.World;
import org.junit.Test;

import java.io.IOException;

/**
 * @author whz
 * @version : ExchangerTest.java, v 0.1 2018-05-21 16:32 whz Exp $$
 */
public class ExchangerTest {

    private static final int port = 9999;
    private Exchanger exchanger = ExtensionLoader.getExtensionLoader(Exchanger.class).getAdaptiveExtension();

    @Test
    public void serverTest() throws RemotingException, IOException {
        // Exchangers.bind(URL.valueOf("exchange://localhost:" + port + "?server=netty"), new TestExchangeHandler());
        exchanger.bind(URL.valueOf("exchange://localhost:" + port + "?server=netty&codec=exchange"), new TestExchangeHandler());
        System.in.read();
    }

    @Test
    public void clientTest() throws RemotingException {
        // ExchangeClient client = Exchangers.connect(URL.valueOf("exchange://localhost:" + port + "?client=netty"), new TestExchangeHandler());
        ExchangeClient client = exchanger.connect(URL.valueOf("exchange://localhost:" + port + "?client=netty&codec=exchange"), new TestExchangeHandler());
        ResponseFuture future = client.request("world");
        String result = (String) future.get();
        System.out.println("客户端接收到的响应信息：" + result);

    }

    class TestExchangeHandler implements ExchangeHandler {

        NettyTransporterTest.TestChannelHandler channelHandler = new NettyTransporterTest.TestChannelHandler();

        /** 如果是通过{@link Exchangers}来创建，服务端或客户端，只需实现该接口即可 */
        @Override
        public Object reply(ExchangeChannel channel, Object request) throws RemotingException {
            System.out.println("接收请求：" + request);
            System.out.println("响应请求：" + "hello world");
            return "hello";
        }

        @Override
        public String telnet(Channel channel, String message) throws RemotingException {
            System.out.println("telnet:" + message);
            return "telnet response";
        }

        @Override
        public void connected(Channel channel) throws RemotingException {
            channelHandler.connected(channel);
        }
        @Override
        public void disconnected(Channel channel) throws RemotingException {
            channelHandler.disconnected(channel);
        }
        @Override
        public void sent(Channel channel, Object message) throws RemotingException {
            channelHandler.sent(channel, message);
        }
        @Override
        public void received(Channel channel, Object message) throws RemotingException {
            channelHandler.received(channel, message);
        }
        @Override
        public void caught(Channel channel, Throwable exception) throws RemotingException {
            channelHandler.caught(channel, exception);
        }

    }
}
