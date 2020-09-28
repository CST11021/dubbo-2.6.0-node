/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.http;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.httpinvoker.HttpComponentsHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.remoting.httpinvoker.SimpleHttpInvokerRequestExecutor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HttpProtocol
 */
public class HttpProtocol extends AbstractProxyProtocol {

    /** http协议的默认端口是80 */
    public static final int DEFAULT_PORT = 80;
    /** key：0.0.0.0:80 */
    private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<String, HttpServer>();
    /**
     * key：/com.alibaba.dubbo.demo.DemoService
     * value：HttpInvokerServiceExporter用于处理请求
     */
    private final Map<String, HttpInvokerServiceExporter> skeletonMap = new ConcurrentHashMap<String, HttpInvokerServiceExporter>();
    /** 通过Dubbo自适应扩展机制复制，默认使用Jetty */
    private HttpBinder httpBinder;

    public HttpProtocol() {
        super(RemoteAccessException.class);
    }



    /**
     * HTTP协议的服务导出：
     *
     * @param impl  该对象为一个代理对象，代理了DemoServiceImpl对象
     * @param type  服务接口类型：com.alibaba.dubbo.demo.DemoService
     * @param url   例如：http://192.168.1.101:80/com.alibaba.dubbo.demo.DemoService?anyhost=true&application=demo-provider&bind.ip=192.168.1.101&bind.port=80&dubbo=2.0.0&generic=false&interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello&pid=12372&side=provider&timestamp=1588412482078
     * @param <T>
     * @return
     * @throws RpcException
     */
    protected <T> Runnable doExport(final T impl, Class<T> type, URL url) throws RpcException {
        // 从url提取ip和端口，例如：ip:port
        String addr = getAddr(url);

        // 获取一个HTTP服务对象，用于接收和处理来自客户端的HTTP请求
        HttpServer server = serverMap.get(addr);
        if (server == null) {
            // 给服务设置相应的请求处理器，用于处理HTTP请求
            server = httpBinder.bind(url, new InternalHandler());
            serverMap.put(addr, server);
        }

        // 将服务调用委托给了String的HttpInvokerServiceExporter类，通过该类，可以通过HTTP的方式调用对应的服务方法
        final HttpInvokerServiceExporter httpServiceExporter = new HttpInvokerServiceExporter();
        httpServiceExporter.setServiceInterface(type);
        httpServiceExporter.setService(impl);
        try {
            httpServiceExporter.afterPropertiesSet();
        } catch (Exception e) {
            throw new RpcException(e.getMessage(), e);
        }

        final String path = url.getAbsolutePath();
        skeletonMap.put(path, httpServiceExporter);
        return new Runnable() {
            public void run() {
                skeletonMap.remove(path);
            }
        };
    }

    /**
     * 服务导出，创建一个代理的服务实现类，通过HTTP请求/响应的方式，发起远程调用
     *
     * @param serviceType   要调用的远程服务接口
     * @param url           远程服务相关信息
     * @param <T>
     * @return
     * @throws RpcException
     */
    @SuppressWarnings("unchecked")
    protected <T> T doRefer(final Class<T> serviceType, final URL url) throws RpcException {
        final HttpInvokerProxyFactoryBean httpProxyFactoryBean = new HttpInvokerProxyFactoryBean();
        // url.toIdentityString()对应：http://192.168.1.101:8080/com.alibaba.dubbo.demo.DemoService
        httpProxyFactoryBean.setServiceUrl(url.toIdentityString());
        httpProxyFactoryBean.setServiceInterface(serviceType);

        String client = url.getParameter(Constants.CLIENT_KEY);


        // 客户端通过HttpInvokerRequestExecutor#executeRequest方法，发起一次HTTP请求，进行服务调用，然后返回服务执行结果
        if (client == null || client.length() == 0 || "simple".equals(client)) {

            SimpleHttpInvokerRequestExecutor httpInvokerRequestExecutor = new SimpleHttpInvokerRequestExecutor() {
                protected void prepareConnection(HttpURLConnection con, int contentLength) throws IOException {
                    super.prepareConnection(con, contentLength);
                    // 接口超时时间默认1秒
                    con.setReadTimeout(url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
                    // 连接超时时间默认3秒
                    con.setConnectTimeout(url.getParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT));
                }
            };

            httpProxyFactoryBean.setHttpInvokerRequestExecutor(httpInvokerRequestExecutor);
        } else if ("commons".equals(client)) {
            HttpComponentsHttpInvokerRequestExecutor httpInvokerRequestExecutor = new HttpComponentsHttpInvokerRequestExecutor();
            httpInvokerRequestExecutor.setReadTimeout(url.getParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT));
            httpProxyFactoryBean.setHttpInvokerRequestExecutor(httpInvokerRequestExecutor);
        } else if (client != null && client.length() > 0) {
            throw new IllegalStateException("Unsupported http protocol client " + client + ", only supported: simple, commons");
        }
        httpProxyFactoryBean.afterPropertiesSet();
        return (T) httpProxyFactoryBean.getObject();
    }

    protected int getErrorCode(Throwable e) {
        if (e instanceof RemoteAccessException) {
            e = e.getCause();
        }
        if (e != null) {
            Class<?> cls = e.getClass();
            if (SocketTimeoutException.class.equals(cls)) {
                return RpcException.TIMEOUT_EXCEPTION;
            } else if (IOException.class.isAssignableFrom(cls)) {
                return RpcException.NETWORK_EXCEPTION;
            } else if (ClassNotFoundException.class.isAssignableFrom(cls)) {
                return RpcException.SERIALIZATION_EXCEPTION;
            }
        }
        return super.getErrorCode(e);
    }

    /**
     * dubbo的自适应扩展机制会调用该方法设置httpBinder对象，默认使用jetty的HttpBinder
     *
     * @param httpBinder
     */
    public void setHttpBinder(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
    }
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    /**
     * 用于处理来自客户端的HTTP请求，服务导出的时候，会将该处理器绑定到对应的URL上
     */
    private class InternalHandler implements HttpHandler {

        /**
         * 该方法用于处理HTTP请求
         *
         * @param request  request.
         * @param response response.
         * @throws IOException
         * @throws ServletException
         */
        public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            // 获取请求的uri，例如：/com.alibaba.dubbo.demo.DemoService
            String uri = request.getRequestURI();
            HttpInvokerServiceExporter skeleton = skeletonMap.get(uri);

            // 必须是post请求
            if (!request.getMethod().equalsIgnoreCase("POST")) {
                response.setStatus(500);
            } else {
                // 在线程上下文件中设置请求来自的客户端地址和端口
                RpcContext.getContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort());
                try {
                    skeleton.handleRequest(request, response);
                } catch (Throwable e) {
                    throw new ServletException(e);
                }
            }
        }

    }

}