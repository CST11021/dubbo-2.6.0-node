package com.whz.dubbo.remoting.http;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.remoting.http.servlet.ServletHttpBinder;
import com.alibaba.dubbo.remoting.http.servlet.ServletHttpServer;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @Author: wanghz
 * @Date: 2020/9/27 10:53 AM
 */
public class HttpServerTest {

    @Test
    public void servletTest() {

        URL url = new URL("http", "localhost", 80);

        HttpBinder binder = new ServletHttpBinder();

        HttpServer httpServer = binder.bind(url, new WhzHttpHandler());

    }

    class WhzHttpHandler implements HttpHandler {

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

            System.out.println(request.getRequestURL());

        }

    }

}
