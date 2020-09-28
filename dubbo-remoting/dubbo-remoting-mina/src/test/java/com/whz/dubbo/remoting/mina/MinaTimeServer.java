package com.whz.dubbo.remoting.mina;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Date;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.SocketAcceptor;

public class MinaTimeServer {

    public static void main(String[] args) throws IOException {
        // 创建IoService的实例
        IoAcceptor acceptor = new SocketAcceptor();
        // 设置过滤链  日志、编解码
        acceptor.getFilterChain().addLast("logger", new LoggingFilter());
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));
        // 让其监听某个端口, 设置处理请求的处理器，通过telnet localhost 9123 命令，来测试
        acceptor.bind(new InetSocketAddress(9123), new TimeServerHandler());
    }

    static class TimeServerHandler extends IoHandlerAdapter {

        /**
         * 接收请求
         *
         * @param session
         * @param message
         */
        public void messageReceived(IoSession session, Object message) {
            String mes = message.toString();
            System.out.println("message writen：" + mes);
            //当接受到的数据是quit时或者用户请求大于2次关闭连接
            if (mes.trim().equalsIgnoreCase("quit")) {
                System.out.println("正在退出时间服务器");
                session.write("正在退出时间服务器。。。。");
                session.close();
                return;
            }

            // 向客户端回复一个当前时间
            session.write(new Date());

        }

    }
}
