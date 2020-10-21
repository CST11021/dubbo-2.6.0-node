package com.whz.dubbo.remoting.mina;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.remoting.*;
import com.alibaba.dubbo.remoting.transport.ChannelHandlerAdapter;
import com.alibaba.dubbo.remoting.transport.mina.MinaClient;
import com.alibaba.dubbo.remoting.transport.mina.MinaServer;
import com.alibaba.dubbo.remoting.transport.mina.MinaTransporter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * @Author: wanghz
 * @Date: 2020/10/7 5:51 PM
 */
public class TransporterTest {

    URL url = URL.valueOf("dubbo://localhost:9123");

    Server minaServer;
    Client minaClient;

    // 初始化Server和Client：开启服务和创建连接
    @Before
    public void Before() throws Exception {
        // 开启服务
        minaServer = new MinaServer(url, new ChannelHandler() {
            @Override
            public void connected(Channel channel) throws RemotingException {
                System.out.println("server:connected");
            }

            @Override
            public void disconnected(Channel channel) throws RemotingException {
                System.out.println("server:disconnected");
            }

            @Override
            public void sent(Channel channel, Object message) throws RemotingException {
                System.out.println("server:sent，message = " + message.toString());
            }

            @Override
            public void received(Channel channel, Object message) throws RemotingException {
                System.out.println("server:received，message = " + message.toString());
            }

            @Override
            public void caught(Channel channel, Throwable exception) throws RemotingException {
                System.out.println("server:caught：" + exception.getMessage());
            }
        });

        // 创建一个连接
        minaClient = new MinaClient(url, new ChannelHandler() {
            @Override
            public void connected(Channel channel) throws RemotingException {
                System.out.println("client:connected");
            }

            @Override
            public void disconnected(Channel channel) throws RemotingException {
                System.out.println("client:disconnected");
            }

            @Override
            public void sent(Channel channel, Object message) throws RemotingException {
                System.out.println("client:sent，message = " + message.toString());
            }

            @Override
            public void received(Channel channel, Object message) throws RemotingException {
                System.out.println("client:received，message = " + message.toString());
            }

            @Override
            public void caught(Channel channel, Throwable exception) throws RemotingException {
                System.out.println("client:caught：" + exception.getMessage());
            }
        });
    }

    // 关闭服务和断开连接
    @After
    public void After() {
        if (minaServer != null) {
            minaServer.close();
        }

        if (minaClient != null) {
            minaClient.close();
        }
    }

    @Test
    public void testServer() throws Exception {


        // 服务确保已经开启
        Assert.assertEquals(true, minaServer.isBound());

        // 获取所有连接该服务通道
        Collection<Channel> channels = minaServer.getChannels();
        Assert.assertEquals(1, channels.size());

        // 获取指定客户端的通道
        // Channel channel = minaServer.getChannel(new InetSocketAddress(9123));
        // Assert.assertNotNull(channel);

        // 向所有的客户端发送消息
        minaServer.send("test123");

        // sleep5秒，以便于能观察到客户端的处理逻辑
        Thread.sleep(5000);

    }

    @Test
    public void testClient() throws Exception {

        Client client = new MinaClient(url, new ChannelHandlerAdapter());
        ((MinaClient) client).disconnect();

        // 测试向服务端发送消息
        client.send("test123");

    }










    // Transporter SPI扩展测试

    @Test
    public void testGetTransportEmpty() {
        try {
            ExtensionLoader.getExtensionLoader(Transporter.class).getExtension("");
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("Extension name == null"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetTransportNull() {
        String name = null;
        ExtensionLoader.getExtensionLoader(Transporter.class).getExtension(name);
    }

    @Test
    public void testGetTransport1() {
        String name = "mina";
        assertEquals(MinaTransporter.class, ExtensionLoader.getExtensionLoader(Transporter.class).getExtension(name).getClass());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetTransportWrong() {
        String name = "nety";
        assertNull(ExtensionLoader.getExtensionLoader(Transporter.class).getExtension(name).getClass());
    }

}
