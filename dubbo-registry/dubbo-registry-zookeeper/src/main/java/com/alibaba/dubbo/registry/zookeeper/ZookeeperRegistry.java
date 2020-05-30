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
package com.alibaba.dubbo.registry.zookeeper;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.support.FailbackRegistry;
import com.alibaba.dubbo.remoting.zookeeper.ChildListener;
import com.alibaba.dubbo.remoting.zookeeper.StateListener;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperClient;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperTransporter;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ZookeeperRegistry:负责与zookeeper进行交互
 *
 */
public class ZookeeperRegistry extends FailbackRegistry {

    private final static Logger logger = LoggerFactory.getLogger(ZookeeperRegistry.class);

    /** ZooKeeper默认端口 */
    private final static int DEFAULT_ZOOKEEPER_PORT = 2181;
    /** ZooKeeper注册中心的默认根节点 */
    private final static String DEFAULT_ROOT = "dubbo";
    /** ZooKeeper用于保存发布的服务信息的根节点，如果设置了group，则根节点为对应的/${group}，否则默认为/dubbo */
    private final String root;
    private final Set<String> anyServices = new ConcurrentHashSet<String>();
    private final ConcurrentMap<URL, ConcurrentMap<NotifyListener, ChildListener>> zkListeners = new ConcurrentHashMap<URL, ConcurrentMap<NotifyListener, ChildListener>>();
    /** 用于操作zk的客户端 */
    private final ZookeeperClient zkClient;


    public ZookeeperRegistry(URL url, ZookeeperTransporter zookeeperTransporter) {
        super(url);
        if (url.isAnyHost()) {
            throw new IllegalStateException("registry address == null");
        }

        // 如果设置了group，则根节点为对应的/${group}，否则默认为/dubbo
        String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_ROOT);
        if (!group.startsWith(Constants.PATH_SEPARATOR)) {
            group = Constants.PATH_SEPARATOR + group;
        }
        this.root = group;
        zkClient = zookeeperTransporter.connect(url);
        zkClient.addStateListener(new StateListener() {
            public void stateChanged(int state) {
                if (state == RECONNECTED) {
                    try {
                        recover();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
    }


    /**
     * 检查zk客户端是否连上zk服务端
     *
     * @return
     */
    public boolean isAvailable() {
        return zkClient.isConnected();
    }
    /**
     * 关闭zk客户端
     */
    public void destroy() {
        super.destroy();
        try {
            zkClient.close();
        } catch (Exception e) {
            logger.warn("Failed to close zookeeper client " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    /**
     * 根据服务消费者的配置从zk获取多个服务提供者信息
     *
     * 查询符合条件的已注册数据，与订阅的推模式相对应，这里为拉模式，只返回一次结果。一个服务可能存在多个服务提供者，所以入参是一个URL，而返回值是一个URL的集合
     *
     *
     * @param url   服务消费者的配合
     * @return
     */
    public List<URL> lookup(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("lookup url == null");
        }

        try {
            List<String> providers = new ArrayList<String>();
            for (String path : toCategoriesPath(url)) {
                List<String> children = zkClient.getChildren(path);
                if (children != null) {
                    providers.addAll(children);
                }
            }
            return toUrlsWithoutEmpty(url, providers);
        } catch (Throwable e) {
            throw new RpcException("Failed to lookup " + url + " from zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }
    /**
     * 注册dubbo服务：在/dubbo/com.alibaba.dubbo.demo.DemoService/providers/节点下以url作为服务节点名创建一个节点
     *
     * dubbo://172.16.120.167:20880/com.alibaba.dubbo.demo.DemoService?
     * anyhost=true
     * &application=demo-provider
     * &dubbo=2.0.0
     * &generic=false
     * &interface=com.alibaba.dubbo.demo.DemoService
     * &methods=sayHello
     * &pid=3487
     * &side=provider
     * &timeout=1000
     * &timestamp=1590046664265
     *
     * @param url
     */
    protected void doRegister(URL url) {
        try {
            // 在ZooKeeper上创建一个节点，将toUrlPath(url)返回的结果保存在ZooKeeper上：
            // 当服务暴露时，节点路径，例如：/dubbo/com.alibaba.dubbo.demo.DemoService/providers/dubbo://172.16.120.167:20880/com.alibaba.dubbo.demo.DemoService?anyhost=true&application=demo-provider&dubbo=2.0.0&generic=false&interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello&pid=3480&side=provider&timeout=1000&timestamp=1590046540467
            // 当服务引用时，节点路径，例如：
            zkClient.create(toUrlPath(url), url.getParameter(Constants.DYNAMIC_KEY, true));
        } catch (Throwable e) {
            throw new RpcException("Failed to register " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }
    /**
     * 删除服务节点
     *
     * @param url
     */
    protected void doUnregister(URL url) {
        try {
            zkClient.delete(toUrlPath(url));
        } catch (Throwable e) {
            throw new RpcException("Failed to unregister " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    /**
     * 订阅服务
     *
     * @param url
     * @param listener
     */
    protected void doSubscribe(final URL url, final NotifyListener listener) {
        try {

            // ================================================================================
            // 服务治理中心 dubbo-admin 会处理所有service层的订阅，service别设置成特殊值*。
            // 此外，服务治理中心除了订阅当前节点，还会订阅这个节点下的所有子节点
            // ================================================================================

            // provider://172.16.120.167:20880/com.alibaba.dubbo.demo.DemoService?
            // anyhost=true&application=demo-provider&category=configurators&check=false&dubbo=2.0.0&generic=false
            // &interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello&pid=5967&side=provider&timeout=1000&timestamp=1590111805356
            if (Constants.ANY_VALUE.equals(url.getServiceInterface())) {
                // 获取根节点
                String root = toRootPath();
                ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
                if (listeners == null) {
                    zkListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, ChildListener>());
                    listeners = zkListeners.get(url);
                }
                ChildListener zkListener = listeners.get(listener);
                if (zkListener == null) {
                    listeners.putIfAbsent(listener, new ChildListener() {
                        public void childChanged(String parentPath, List<String> currentChilds) {
                            for (String child : currentChilds) {
                                child = URL.decode(child);
                                if (!anyServices.contains(child)) {
                                    anyServices.add(child);
                                    subscribe(url.setPath(child).addParameters(Constants.INTERFACE_KEY, child,
                                            Constants.CHECK_KEY, String.valueOf(false)), listener);
                                }
                            }
                        }
                    });
                    zkListener = listeners.get(listener);
                }
                zkClient.create(root, false);
                List<String> services = zkClient.addChildListener(root, zkListener);
                if (services != null && services.size() > 0) {
                    for (String service : services) {
                        service = URL.decode(service);
                        anyServices.add(service);
                        subscribe(url.setPath(service).addParameters(Constants.INTERFACE_KEY, service,
                                Constants.CHECK_KEY, String.valueOf(false)), listener);
                    }
                }
            }



            // =============================
            // 普通消费者的订阅逻辑。
            // 首先根据URL的类别得到一组需要订阅的路径。如果类别是*，则会订阅4种类型的路径（providers、routers、consumers、configurators），否则值订阅providers路径
            // =============================



            else {
                List<URL> urls = new ArrayList<URL>();
                // 根据url的category，获取一组需要订阅的路径，path例如：
                // /dubbo/com.alibaba.dubbo.demo.DemoService/providers
                // /dubbo/com.alibaba.dubbo.demo.DemoService/consumers
                // /dubbo/com.alibaba.dubbo.demo.DemoService/routers
                // /dubbo/com.alibaba.dubbo.demo.DemoService/configurators
                for (String path : toCategoriesPath(url)) {
                    ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
                    // 如果listeners缓存为空则创建缓存
                    if (listeners == null) {
                        zkListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, ChildListener>());
                        listeners = zkListeners.get(url);
                    }

                    ChildListener zkListener = listeners.get(listener);
                    if (zkListener == null) {
                        listeners.putIfAbsent(listener, new ChildListener() {
                            /**
                             * 当path子节点变更时，会调用该方法
                             *
                             * @param parentPath
                             * @param currentChilds
                             */
                            public void childChanged(String parentPath, List<String> currentChilds) {
                                ZookeeperRegistry.this.notify(url, listener, toUrlsWithEmpty(url, parentPath, currentChilds));
                            }
                        });
                        zkListener = listeners.get(listener);
                    }

                    // 创建持久节点，例如：
                    // /dubbo/com.alibaba.dubbo.demo.DemoService/providers
                    // /dubbo/com.alibaba.dubbo.demo.DemoService/consumers
                    // /dubbo/com.alibaba.dubbo.demo.DemoService/routers
                    // /dubbo/com.alibaba.dubbo.demo.DemoService/configurators
                    zkClient.create(path, false);

                    // 给该该节点添加子节点监听器，并返回该路径下的所有子节点
                    List<String> children = zkClient.addChildListener(path, zkListener);
                    if (children != null) {
                        urls.addAll(toUrlsWithEmpty(url, path, children));
                    }
                }

                // 回调notifyListener，更新本地缓存信息
                // 注意，此处会根据URL中的category属性值获取具体的类别：providers、routers、consumers、configurators，然后拉取直接子节点的数据进行通知（notify）。
                // 如果是providers类别的数据，则订阅方会更新本地Directory管理的Invoker服务列表；
                // 如果是routers分类，则订阅方会更新本地路由规则列表；
                // 如果是configuators类别，则订阅方会更新或覆盖本地动态参数列表
                notify(url, listener, urls);

            }




        } catch (Throwable e) {
            throw new RpcException("Failed to subscribe " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
        if (listeners != null) {
            ChildListener zkListener = listeners.get(listener);
            if (zkListener != null) {
                zkClient.removeChildListener(toUrlPath(url), zkListener);
            }
        }
    }


    static String appendDefaultPort(String address) {
        if (address != null && address.length() > 0) {
            int i = address.indexOf(':');
            if (i < 0) {
                return address + ":" + DEFAULT_ZOOKEEPER_PORT;
            } else if (Integer.parseInt(address.substring(i + 1)) == 0) {
                return address.substring(0, i + 1) + DEFAULT_ZOOKEEPER_PORT;
            }
        }
        return address;
    }

    /**
     * 给zk的根节点添加/
     *
     * @return
     */
    private String toRootDir() {
        if (root.equals(Constants.PATH_SEPARATOR)) {
            return root;
        }
        return root + Constants.PATH_SEPARATOR;
    }

    /**
     * ZooKeeper用于保存发布的服务信息的根节点，如果设置了group，则根节点为对应的/${group}，否则默认为/dubbo
     *
     * @return
     */
    private String toRootPath() {
        return root;
    }

    /**
     * 获取服务在zk上的路径，例如：/dubbo/com.alibaba.dubbo.demo.DemoService
     *
     * @param url
     * @return
     */
    private String toServicePath(URL url) {
        String name = url.getServiceInterface();
        if (Constants.ANY_VALUE.equals(name)) {
            return toRootPath();
        }
        return toRootDir() + URL.encode(name);
    }

    /**
     * 根据url的category，获取一组需要订阅的路径，例如：
     * /dubbo/com.alibaba.dubbo.demo.DemoService/providers
     * /dubbo/com.alibaba.dubbo.demo.DemoService/consumers
     * /dubbo/com.alibaba.dubbo.demo.DemoService/routers
     * /dubbo/com.alibaba.dubbo.demo.DemoService/configurators
     *
     * @param url
     * @return
     */
    private String[] toCategoriesPath(URL url) {
        String[] categroies;
        // category = "*"
        if (Constants.ANY_VALUE.equals(url.getParameter(Constants.CATEGORY_KEY))) {
            // providers、consumers、routers和configurators
            categroies = new String[]{
                    Constants.PROVIDERS_CATEGORY,
                    Constants.CONSUMERS_CATEGORY,
                    Constants.ROUTERS_CATEGORY,
                    Constants.CONFIGURATORS_CATEGORY
            };
        } else {
            categroies = url.getParameter(Constants.CATEGORY_KEY, new String[]{Constants.DEFAULT_CATEGORY});
        }

        String[] paths = new String[categroies.length];
        for (int i = 0; i < categroies.length; i++) {
            // 例如：/dubbo/com.alibaba.dubbo.demo.DemoService/configurators
            paths[i] = toServicePath(url) + Constants.PATH_SEPARATOR + categroies[i];
        }
        return paths;
    }

    /**
     * 获取category路径：例如：
     * /dubbo/com.alibaba.dubbo.demo.DemoService/providers
     * /dubbo/com.alibaba.dubbo.demo.DemoService/providers
     * /dubbo/com.alibaba.dubbo.demo.DemoService/*
     * @param url
     * @return
     */
    private String toCategoryPath(URL url) {
        return toServicePath(url) + Constants.PATH_SEPARATOR + url.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
    }

    /**
     * 例如：dubbo://172.16.120.167:20880/com.alibaba.dubbo.demo.DemoService?anyhost=true&application=demo-provider&dubbo=2.0.0
     * &generic=false&interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello&pid=3343&side=provider&timeout=1000&timestamp=1590044146566
     *
     * @param url
     * @return
     */
    private String toUrlPath(URL url) {
        return toCategoryPath(url) + Constants.PATH_SEPARATOR + URL.encode(url.toFullString());
    }

    /**
     * 返回该消费者可用的dubbo服务的url，如果没有可用的服务返回一个：empty协议的url
     *
     * @param consumer          例如：consumer://172.16.120.167/com.alibaba.dubbo.demo.DemoService?application=demo-consumer&category=providers,configurators,routers&check=false&dubbo=2.0.0&interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello&pid=3507&side=consumer&stub=whz.stub.DemoServiceStub&timestamp=1590047311243
     * @param providers         例如：dubbo://172.16.120.167:20880/com.alibaba.dubbo.demo.DemoService?anyhost=true&application=demo-provider&dubbo=2.0.0&generic=false&interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello&pid=3497&side=provider&timeout=1000&timestamp=1590047231690
     * @return  例如：dubbo://172.16.120.167:20880/com.alibaba.dubbo.demo.DemoService?anyhost=true&application=demo-provider&dubbo=2.0.0&generic=false&interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello&pid=3497&side=provider&timeout=1000&timestamp=1590047231690
     */
    private List<URL> toUrlsWithoutEmpty(URL consumer, List<String> providers) {
        List<URL> urls = new ArrayList<URL>();
        if (providers != null && providers.size() > 0) {
            for (String provider : providers) {
                provider = URL.decode(provider);
                if (provider.contains("://")) {
                    URL url = URL.valueOf(provider);
                    if (UrlUtils.isMatch(consumer, url)) {
                        urls.add(url);
                    }
                }
            }
        }
        return urls;
    }

    /**
     * 返回该消费者可用的dubbo服务的url，如果没有可用的服务返回一个：empty协议的url
     *
     * @param consumer
     * @param path
     * @param providers
     * @return  例如：dubbo://172.16.120.167:20880/com.alibaba.dubbo.demo.DemoService?anyhost=true&application=demo-provider&dubbo=2.0.0&generic=false&interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello&pid=3497&side=provider&timeout=1000&timestamp=1590047231690
     */
    private List<URL> toUrlsWithEmpty(URL consumer, String path, List<String> providers) {
        List<URL> urls = toUrlsWithoutEmpty(consumer, providers);
        if (urls == null || urls.isEmpty()) {
            int i = path.lastIndexOf('/');
            String category = i < 0 ? path : path.substring(i + 1);
            URL empty = consumer.setProtocol(Constants.EMPTY_PROTOCOL).addParameter(Constants.CATEGORY_KEY, category);
            urls.add(empty);
        }
        return urls;
    }

}