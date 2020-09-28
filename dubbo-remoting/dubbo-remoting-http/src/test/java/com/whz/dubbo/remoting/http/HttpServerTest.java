package com.whz.dubbo.remoting.http;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.remoting.http.jetty.JettyHttpBinder;
import com.alibaba.dubbo.remoting.http.jetty.JettyHttpServer;
import com.alibaba.dubbo.remoting.http.servlet.ServletHttpBinder;
import com.alibaba.dubbo.remoting.http.servlet.ServletHttpServer;
import com.alibaba.dubbo.remoting.http.tomcat.TomcatHttpBinder;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;

/**
 * @Author: wanghz
 * @Date: 2020/9/27 10:53 AM
 */
public class HttpServerTest {

    URL url = new URL("http", "localhost", 80);

    /**
     * 开启一个Jetty服务，用于接收HTTP请求
     *
     * @throws IOException
     */
    @Test
    public void JettyHttpServerStartTest() throws IOException {

        HttpBinder binder = new JettyHttpBinder();
        HttpServer httpServer = binder.bind(url, new WhzHttpHandler());
        System.in.read();
    }

    /**
     * 开启一个Tomcat服务，用于接收HTTP请求
     *
     * @throws IOException
     */
    @Test
    public void TomcatHttpServerStartTest() throws IOException {
        HttpBinder binder = new TomcatHttpBinder();
        HttpServer httpServer = binder.bind(url, new WhzHttpHandler());
        System.in.read();
    }

    class WhzHttpHandler implements HttpHandler {

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            System.out.println("收到来自客户端的请求：" + request.getRequestURL());
        }

    }

}
