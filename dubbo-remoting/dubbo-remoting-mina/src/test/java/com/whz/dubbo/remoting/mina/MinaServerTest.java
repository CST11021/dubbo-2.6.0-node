package com.whz.dubbo.remoting.mina;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.mina.MinaClient;
import com.alibaba.dubbo.remoting.transport.mina.MinaServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * @Author: wanghz
 * @Date: 2020/10/7 5:51 PM
 */
public class MinaServerTest {

    URL url = URL.valueOf("dubbo://localhost:9123");

    ChannelHandler handler = new ChannelHandler() {
        @Override
        public void connected(Channel channel) throws RemotingException {
            System.out.println("当客户端与服务端建立连接时调用");
        }

        @Override
        public void disconnected(Channel channel) throws RemotingException {
            System.out.println("当客户端与服务端断开连接时调用");
        }

        @Override
        public void sent(Channel channel, Object message) throws RemotingException {
            System.out.println("当向通道发送消息的时候调用，message = " + message.toString());
        }

        @Override
        public void received(Channel channel, Object message) throws RemotingException {
            System.out.println("当从通道接收到消息的时候调用，message = " + message.toString());
        }

        @Override
        public void caught(Channel channel, Throwable exception) throws RemotingException {
            System.out.println("异常回调：" + exception.getMessage());
        }
    };

    MinaServer minaServer;
    MinaClient minaClient;

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
    public void send() throws Exception {

        // 服务确保已经开启
        Assert.assertEquals(true, minaServer.isBound());

        // 获取所有连接该服务通道
        Collection<Channel> channels = minaServer.getChannels();

        // 获取指定客户端的通道
        Channel channel = minaServer.getChannel(new InetSocketAddress(9123));

        // 向所有的客户端发送消息
        minaServer.send("test123");

        Thread.sleep(5000);
    }

}
