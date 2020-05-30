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
package com.alibaba.dubbo.registry.integration;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryFactory;
import com.alibaba.dubbo.registry.RegistryService;
import com.alibaba.dubbo.registry.support.ProviderConsumerRegTable;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.cluster.Configurator;
import com.alibaba.dubbo.rpc.protocol.InvokerWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RegistryProtocol
 * 服务地址发布到注册中心，注册中心通过{@link #getRegistry(Invoker)}获取
 *
 */
public class RegistryProtocol implements Protocol {

    private final static Logger logger = LoggerFactory.getLogger(RegistryProtocol.class);
    /** 表示注册中心协议本身 */
    private static RegistryProtocol INSTANCE;
    /** 用于监听 /dubbo/com.alibaba.dubbo.demo.DemoService/configurators 的子节点变更，服务导出时，会添加该服务对应的监听器 */
    private final Map<URL, NotifyListener> overrideListeners = new ConcurrentHashMap<URL, NotifyListener>();
    /**
     * 为了解决RMI重复导出，导致端口冲突的问题，将已暴露的服务缓存起来，防止重复导出，key：{@link URL#toFullString()} ，value：{@link Exporter}
        通过协议暴露后会返回一个Exporter对象，然后包装为一个ExporterChangeableWrapper对象缓存到这里，详见：{@link #doLocalExport(Invoker)}方法
     */
    private final Map<String, ExporterChangeableWrapper<?>> bounds = new ConcurrentHashMap<String, ExporterChangeableWrapper<?>>();


    // 以下，通过{@link ExtensionLoader#injectExtension(Object)}方法实现自动注入

    /** 用于将多个Invoker伪装为一个 */
    private Cluster cluster;
    /** 真正的服务导出协议，比如：dubbo、HTTP协议等，在将服务注册到注册中心前，会先通过该对象导出服务 */
    private Protocol protocol;
    /** 用于构建注册中心实现的工厂类 */
    private RegistryFactory registryFactory;
    private ProxyFactory proxyFactory;










    public RegistryProtocol() {
        INSTANCE = this;
    }
    public static RegistryProtocol getRegistryProtocol() {
        if (INSTANCE == null) {
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(Constants.REGISTRY_PROTOCOL);
        }
        return INSTANCE;
    }








    // ===== 服务导出 =====



    /**
     * 主要做如下一些操作：
     *
     * 1、调用 doLocalExport 导出服务
     * 2、向注册中心注册服务
     * 3、向注册中心进行订阅 override 数据
     * 4、创建并返回 DestroyableExporter
     *
     * 注册时应该是将 registedProviderUrl 传递到注册中心，注册中心记录相应信息；这里我们可以理解为，消费者访问注册中心时，
     * 根据消费者需要获得的服务去读取服务提供者（url）；而订阅时，则是根据 overrideSubscribeUrl 地址和 overrideSubscribeListener
     * 监听。overrideSubscribeListener 监听的作用是当提供者的 url 改变时，重新 export；
     *
     *
     * @param originInvoker
     * @param <T>
     * @return
     * @throws RpcException
     */
    public <T> Exporter<T> export(final Invoker<T> originInvoker) throws RpcException {




        // ======= export invoker：主要是打开socket侦听服务，并接收客户端发来的各种请求 ======
        final ExporterChangeableWrapper<T> exporter = doLocalExport(originInvoker);



        // ==== 创建注册中心 ====



        // 获取注册中心 URL，以 zookeeper 注册中心为例，得到的示例 URL 如下：
        // zookeeper://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService?
        // application=demo-provider
        // &dubbo=2.0.2
        // &export=dubbo://172.17.48.52:20880/com.alibaba.dubbo.demo.DemoService?anyhost=true&application=demo-provider
        URL registryUrl = getRegistryUrl(originInvoker);
        // 根据 URL 加载 Registry 实现类，比如 ZookeeperRegistry
        final Registry registry = getRegistry(originInvoker);




        // ======== 注册服务 =======


        // 获取已注册的服务提供者 URL，比如：
        // dubbo://172.17.48.52:20880/com.alibaba.dubbo.demo.DemoService
        // ?anyhost=true
        // &application=demo-provider
        // &dubbo=2.0.2
        // &generic=false
        // &interface=com.alibaba.dubbo.demo.DemoService
        // &methods=sayHello
        final URL registedProviderUrl = getRegistedProviderUrl(originInvoker);

        // 是否将服务提供者的信息把保存到注册中心
        boolean register = registedProviderUrl.getParameter("register", true);

        // 向服务提供者与消费者注册表中注册服务提供者
        ProviderConsumerRegTable.registerProvider(originInvoker, registryUrl, registedProviderUrl);

        // 根据 register 的值决定是否注册服务到zk
        if (register) {
            // 使用registryUrl创建一个注册中心，并向注册中心注册服务
            register(registryUrl, registedProviderUrl);
            // 本地注册表标记为已经注册
            ProviderConsumerRegTable.getProviderWrapper(originInvoker).setReg(true);
        }



        // ==== 订阅服务 ====


        //

        // Subscribe the override data
        // FIXME： When the provider subscribes, it will affect the scene : a certain JVM exposes the service and call the same service.
        // FIXME：Because the subscribed is cached key with the name of the service, it causes the subscription information to cover.
        // FIXME： 提供者订阅时，会影响同一JVM即暴露服务，又引用同一服务的的场景，因为subscribed以服务名为缓存的key，导致订阅信息覆盖。
        // 获取订阅 URL，将协议改为provider，并添加 &category=configurators&check=false 参数，比如：
        // provider://172.17.48.52:20880/com.alibaba.dubbo.demo.DemoService
        // ?category=configurators
        // &check=false
        // &anyhost=true
        // &application=demo-provider
        // &dubbo=2.0.2
        // &generic=false
        // &interface=com.alibaba.dubbo.demo.DemoService
        // &methods=sayHello
        final URL overrideSubscribeUrl = getSubscribedOverrideUrl(registedProviderUrl);
        // 创建一个监听器
        final OverrideListener overrideSubscribeListener = new OverrideListener(overrideSubscribeUrl, originInvoker);
        // 本地保存url对应的监听器
        overrideListeners.put(overrideSubscribeUrl, overrideSubscribeListener);
        // 监听：/dubbo/com.alibaba.dubbo.demo.DemoService/configurators 的子节点变更：
        registry.subscribe(overrideSubscribeUrl, overrideSubscribeListener);

        // 保证每次export都返回一个新的exporter实例
        return new Exporter<T>() {
            public Invoker<T> getInvoker() {
                return exporter.getInvoker();
            }
            public void unexport() {
                // 关闭本地服务
                try {
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }

                // 注册中心注销该服务
                try {
                    registry.unregister(registedProviderUrl);
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }

                // 取消暴露时，移除监听
                try {
                    overrideListeners.remove(overrideSubscribeUrl);
                    registry.unsubscribe(overrideSubscribeUrl, overrideSubscribeListener);
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        };
    }
    /**
     * 暴露服务，并将暴露的服务缓存到本地，Dubbo协议的Invoker转为Exporter发生在DubboProtocol类的export方法，它主要是打开socket侦听服务，并接收客户端的请求
     *
     * @param originInvoker
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T> ExporterChangeableWrapper<T> doLocalExport(final Invoker<T> originInvoker) {
        // 这里originInvoker的协议是registry,url例如：
        // registry://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService
        // ?application=demo-provider
        // &dubbo=2.0.0
        // &export=dubbo://172.16.110.158:20880/com.alibaba.dubbo.demo.DemoService
        // ?anyhost=true
        // &application=demo-provider
        // &bind.ip=172.16.110.158&bind.port=20880
        // &dubbo=2.0.0
        // &generic=false
        // &interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello
        // &pid=33230
        // &side=provider
        // &timestamp=1569031709278
        // &pid=33230
        // &registry=zookeeper
        // &timestamp=1569031708581

        // --------------------------------------------------------------------------------

        // 这里的缓存key直接使用URL的fullString，例如：dubbo://30.6.28.128:20880/com.alibaba.dubbo.demo.DemoService
        // ?anyhost=true
        // &application=demo-provider
        // &bind.ip=30.6.28.128
        // &bind.port=20880
        // &dubbo=2.0.0
        // &generic=false
        // &interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello
        // &pid=1225680
        // &side=provider
        // &timestamp=1522221254113
        String key = getCacheKey(originInvoker);
        ExporterChangeableWrapper<T> exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
        if (exporter == null) {
            synchronized (bounds) {
                exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
                if (exporter == null) {
                    // invokerDelegete的url即为getProviderUrl()方法返回的URL，这里是将registry协议转为dubbo协议，url例如：
                    // dubbo://172.16.110.158:20880/com.alibaba.dubbo.demo.DemoService
                    // ?anyhost=true
                    // &application=demo-provider
                    // &bind.ip=172.16.110.158
                    // &bind.port=20880
                    // &dubbo=2.0.0
                    // &generic=false
                    // &interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello
                    // &pid=33230
                    // &side=provider
                    // &timestamp=1569031709278
                    final Invoker<?> invokerDelegete = new InvokerDelegete<T>(originInvoker, getProviderUrl(originInvoker));
                    // 使用最终指向的协议暴露服务，比如DubboProtocol
                    exporter = new ExporterChangeableWrapper<T>((Exporter<T>) protocol.export(invokerDelegete), originInvoker);
                    bounds.put(key, exporter);
                }
            }
        }
        return exporter;
    }
    /**
     * 获取调用程序在范围内缓存的key
     *
     * @param originInvoker
     * @return
     */
    private String getCacheKey(final Invoker<?> originInvoker) {
        // 获取服务提供者的配置信息，其配置信息封装为URL对象返回
        URL providerUrl = getProviderUrl(originInvoker);
        String key = providerUrl.removeParameters("dynamic", "enabled").toFullString();
        return key;
    }
    /**
     * 返回注册中心相关的配置信息：获取originInvoker的url并设置协议为registry协议
     *
     * @param originInvoker
     * @return
     */
    private URL getRegistryUrl(Invoker<?> originInvoker) {
        // registry://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService
        // ?application=demo-provider
        // &dubbo=2.0.0
        // &export=dubbo://172.16.120.147:20880/com.alibaba.dubbo.demo.DemoService?anyhost=true&application=demo-provider&bind.ip=172.16.120.147&bind.port=20880&dubbo=2.0.0&generic=false&interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello&pid=27660&side=provider&timeout=1000&timestamp=1589350902783&pid=27660&registry=zookeeper&timestamp=1589350902756
        URL registryUrl = originInvoker.getUrl();
        if (Constants.REGISTRY_PROTOCOL.equals(registryUrl.getProtocol())) {
            String protocol = registryUrl.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_DIRECTORY);
            registryUrl = registryUrl.setProtocol(protocol).removeParameter(Constants.REGISTRY_KEY);
        }
        return registryUrl;
    }
    /**
     * Get an instance of registry based on the address of invoker
     * 通过{@link #registryFactory}创建一个注册中心对象
     *
     * @param originInvoker
     * @return
     */
    private Registry getRegistry(final Invoker<?> originInvoker) {
        URL registryUrl = getRegistryUrl(originInvoker);
        return registryFactory.getRegistry(registryUrl);
    }
    /**
     * Return the url that is registered to the registry and filter the url parameter once
     *
     * @param originInvoker
     * @return
     */
    private URL getRegistedProviderUrl(final Invoker<?> originInvoker) {
        URL providerUrl = getProviderUrl(originInvoker);
        //The address you see at the registry
        final URL registedProviderUrl = providerUrl.removeParameters(getFilteredKeys(providerUrl))
                .removeParameter(Constants.MONITOR_KEY)
                .removeParameter(Constants.BIND_IP_KEY)
                .removeParameter(Constants.BIND_PORT_KEY);
        return registedProviderUrl;
    }
    /**
     * 过滤不需要在url中输出的参数（以"."开头）
     */
    private static String[] getFilteredKeys(URL url) {
        Map<String, String> params = url.getParameters();
        if (params != null && !params.isEmpty()) {
            List<String> filteredKeys = new ArrayList<String>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry != null && entry.getKey() != null && entry.getKey().startsWith(Constants.HIDE_KEY_PREFIX)) {
                    filteredKeys.add(entry.getKey());
                }
            }
            return filteredKeys.toArray(new String[filteredKeys.size()]);
        } else {
            return new String[]{};
        }
    }
    /**
     * Get the address of the providerUrl through the url of the invoker
     * 获取服务提供者的配置信息，其配置信息封装为URL对象返回
     *
     * @param origininvoker
     * @return 例如：dubbo://172.16.110.158:20880/com.alibaba.dubbo.demo.DemoService
     * ?anyhost=true
     * &application=demo-provider
     * &bind.ip=172.16.110.158
     * &bind.port=20880
     * &dubbo=2.0.0
     * &generic=false
     * &interface=com.alibaba.dubbo.demo.DemoService
     * &methods=sayHello
     * &pid=33230
     * &side=provider
     * &timestamp=1569031709278
     */
    private URL getProviderUrl(final Invoker<?> origininvoker) {
        String export = origininvoker.getUrl().getParameterAndDecoded(Constants.EXPORT_KEY);
        if (export == null || export.length() == 0) {
            throw new IllegalArgumentException("The registry export url is null! registry: " + origininvoker.getUrl());
        }

        URL providerUrl = URL.valueOf(export);
        return providerUrl;
    }


    /**
     * 将协议改为provider，并添加 &category=configurators&check=false 参数
     *
     * registedProviderUrl，比如入参：dubbo://172.17.48.52:20880/com.alibaba.dubbo.demo.DemoService
     *     ?anyhost=true
     *     &application=demo-provider
     *     &dubbo=2.0.2
     *     &generic=false
     *     &interface=com.alibaba.dubbo.demo.DemoService
     *     &methods=sayHello
     *
     * 出参：provider//172.17.48.52:20880/com.alibaba.dubbo.demo.DemoService
     *     ?anyhost=true
     *     &application=demo-provider
     *     &dubbo=2.0.2
     *     &generic=false
     *     &interface=com.alibaba.dubbo.demo.DemoService
     *     &methods=sayHello
     *     &category=configurators
     *     &check=false
     *
     * @param registedProviderUrl
     * @return
     */
    private URL getSubscribedOverrideUrl(URL registedProviderUrl) {
        return registedProviderUrl.setProtocol(Constants.PROVIDER_PROTOCOL)
                .addParameters(
                        Constants.CATEGORY_KEY, Constants.CONFIGURATORS_CATEGORY,
                        Constants.CHECK_KEY, String.valueOf(false)
                );
    }
    /**
     * 当/dubbo/com.alibaba.dubbo.demo.DemoService/configurators的子节点变更时，会将配置的规则应用到服务url中并生产新的url，再通过
     * 该方法重新导出
     *
     * @param originInvoker
     * @param newInvokerUrl
     */
    @SuppressWarnings("unchecked")
    private <T> void doChangeLocalExport(final Invoker<T> originInvoker, URL newInvokerUrl) {
        String key = getCacheKey(originInvoker);
        final ExporterChangeableWrapper<T> exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
        if (exporter == null) {
            logger.warn(new IllegalStateException("error state, exporter should not be null"));
        } else {
            final Invoker<T> invokerDelegete = new InvokerDelegete<T>(originInvoker, newInvokerUrl);
            exporter.setExporter(protocol.export(invokerDelegete));
        }
    }

    /**
     * 向注册中心暴露服务
     *
     * @param registryUrl               用于创建注册中心的url
     * @param registedProviderUrl       向注册中注册服务的url
     */
    public void register(URL registryUrl, URL registedProviderUrl) {
        // 根据配置的注册中心信息registryUrl，创建一个注册中心，registryUrl例如：
        // multicast://224.5.6.7:1234/com.alibaba.dubbo.registry.RegistryService
        // ?application=demo-provider&dubbo=2.0.0
        // &export=dubbo://192.168.85.1:20880/com.alibaba.dubbo.demo.DemoService?anyhost=true&application=demo-provider&bind.ip=192.168.85.1&bind.port=20880&dubbo=2.0.0&generic=false&interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello&pid=5892&side=provider&timestamp=1526286422659&pid=5892&timestamp=1526286422635
        Registry registry = registryFactory.getRegistry(registryUrl);
        // 根据服务提供者的信息向注册中心暴露服务，registedProviderUrl例如：
        // dubbo://192.168.85.1:20880/com.alibaba.dubbo.demo.DemoService?
        // anyhost=true&application=demo-provider&dubbo=2.0.0&generic=false&interface=com.alibaba.dubbo.demo.DemoService
        // &methods=sayHello&pid=5892&side=provider&timestamp=1526286422659
        registry.register(registedProviderUrl);
    }




    // ===== 服务导入 =====



    /**
     * 从注册中心引入服务
     *
     * @param type              服务的类型
     * @param url               远程服务的URL地址，该URL也包含了注册中心的配置信息
     * @param <T>
     * @return
     * @throws RpcException
     */
    @SuppressWarnings("unchecked")
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {

        // ===== 将url转为创建注册中心的url，然后创建注册中心实例 =====

        // 注册中心的协议，例如：url.setProtocol("zookeeper")
        url = url.setProtocol(url.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_REGISTRY)).removeParameter(Constants.REGISTRY_KEY);
        // 获取注册中心
        Registry registry = registryFactory.getRegistry(url);




        if (RegistryService.class.equals(type)) {
            return proxyFactory.getInvoker((T) registry, type, url);
        }




        // ======= group="a,b" or group="*" 情况的服务导出 =======

        Map<String, String> qs = StringUtils.parseQueryString(url.getParameterAndDecoded(Constants.REFER_KEY));
        String group = qs.get(Constants.GROUP_KEY);
        if (group != null && group.length() > 0) {
            if ((Constants.COMMA_SPLIT_PATTERN.split(group)).length > 1 || "*".equals(group)) {
                return doRefer(getMergeableCluster(), registry, type, url);
            }
        }



        // url:zookeeper://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService
        // ?application=demo-consumer&dubbo=2.0.0&pid=2171&refer=application=demo-consumer&check=false&dubbo=2.0.0
        // &interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello&pid=2171&register.ip=192.168.1.101&side=consumer&stub=whz.stub.DemoServiceStub&timestamp=1589988301764&timestamp=1589988301780
        return doRefer(cluster, registry, type, url);
    }
    /**
     * 获取这个服务的{@link Invoker}对象
     *
     * @param cluster       集群容错策略
     * @param registry      注册中心
     * @param type          服务接口
     * @param url           服务对应的URL
     * @param <T>
     * @return
     */
    private <T> Invoker<T> doRefer(Cluster cluster, Registry registry, Class<T> type, URL url) {

        // ===== 创建服务目录 =====

        // 服务目录的作用：每一个服务提供者对应一个invoker，多个服务提供者就会对应多个invoker。
        // 如果一个消费者对应多个提供者，dubbo会把多个invoker合并成一个invoker来处理，具体处理逻辑是：
        // 根据服务名去注册中心获取多个服务地址，服务地址经过路由器过滤掉一部分地址，剩下的地址称为服务目录的invoker合并，真正调用的时候才选取一个再进行负载均衡和容错处理。
        RegistryDirectory<T> directory = new RegistryDirectory<T>(type, url);
        directory.setRegistry(registry);
        directory.setProtocol(protocol);




        // ===== 向注册中心注册消费者的服务引用 =====


        // all attributes of REFER_KEY
        Map<String, String> parameters = new HashMap<String, String>(directory.getUrl().getParameters());
        // consumer协议、注册中心IP，接口名
        URL subscribeUrl = new URL(Constants.CONSUMER_PROTOCOL, parameters.remove(Constants.REGISTER_IP_KEY), 0, type.getName(), parameters);
        if (!Constants.ANY_VALUE.equals(url.getServiceInterface()) && url.getParameter(Constants.REGISTER_KEY, true)) {
            // 向注册中心注册消费者要订阅的服务等相关信息，subscribeUrl示例如下：
            // consumer://192.168.1.101/com.alibaba.dubbo.demo.DemoService
            // ?application=demo-consumer
            // &check=false
            // &dubbo=2.0.0
            // &interface=com.alibaba.dubbo.demo.DemoService
            // &methods=sayHello
            // &pid=2186
            // &side=consumer
            // &stub=whz.stub.DemoServiceStub
            // &timestamp=1589988961558

            // 服务消费者启动时，向 /dubbo/com.foo.BarService/consumers 目录下写入自己的 URL 地址
            registry.register(
                    // 添加 category=consumers%check=false 参数
                    subscribeUrl.addParameters(
                            Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY,
                            Constants.CHECK_KEY, String.valueOf(false)
                    )
            );
        }



        // ==== 订阅服务导入的url ====


        // 服务目录会订阅zk上的可用服务，并动态更新服务目录
        // 程序走到这里会去调用相应协议实现的refer方法，比如：DubboProtocol#refer()方法
        // 添加单个参数：category=providers,configurators,routers
        // 服务消费者启动时，订阅 /dubbo/com.foo.BarService/providers 目录下的提供者 URL 地址
        directory.subscribe(subscribeUrl.addParameter(Constants.CATEGORY_KEY,
                Constants.PROVIDERS_CATEGORY + "," + Constants.CONFIGURATORS_CATEGORY + "," + Constants.ROUTERS_CATEGORY));



        // ====== 将 Directory 中的多个 Invoker 伪装成一个 Invoker, 对上层透明，包含集群的容错机制 ======
        Invoker invoker = cluster.join(directory);




        // ==== 本地标记该url为已注册 ====

        ProviderConsumerRegTable.registerConsuemr(invoker, url, subscribeUrl, directory);
        return invoker;
    }
    private Cluster getMergeableCluster() {
        return ExtensionLoader.getExtensionLoader(Cluster.class).getExtension("mergeable");
    }













    public int getDefaultPort() {
        return 9090;
    }
    public void destroy() {
        List<Exporter<?>> exporters = new ArrayList<Exporter<?>>(bounds.values());
        for (Exporter<?> exporter : exporters) {
            exporter.unexport();
        }
        bounds.clear();
    }















    // getter and setter ...


    public Map<URL, NotifyListener> getOverrideListeners() {
        return overrideListeners;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }
    public void setRegistryFactory(RegistryFactory registryFactory) {
        this.registryFactory = registryFactory;
    }
    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }















    public static class InvokerDelegete<T> extends InvokerWrapper<T> {
        private final Invoker<T> invoker;

        /**
         * @param invoker
         * @param url     invoker.getUrl return this value
         */
        public InvokerDelegete(Invoker<T> invoker, URL url) {
            super(invoker, url);
            this.invoker = invoker;
        }

        public Invoker<T> getInvoker() {
            if (invoker instanceof InvokerDelegete) {
                return ((InvokerDelegete<T>) invoker).getInvoker();
            } else {
                return invoker;
            }
        }
    }

    /**
     * 用于监听：/dubbo/com.alibaba.dubbo.demo.DemoService/configurators 的子节点变更：
     * 其中动态配置时放在configurators节点目录下。服务消费端会监听 configurators 目录变更，如果变更则会调用
     * RegistryDirectory#notify(List<URL> urls)方法。监听configurators目录变更触发的void notify(List<URL> urls)方法时，
     * urls的是类似override://...，表示将覆盖调用该服务的某些配置(dubbo中对所有的调用配置都是通过URL的形式来展示的)，
     * 将这些URL上面的参数信息替换到调用服务端的URL上面取，并且重新构造该服务的Invoke对象，从而达到更新参数的目的。
     *
     *
     * 注册器{@link Registry}会向注册中心订阅服务，当注册中心的服务信息发生变化时，会使用该接口进行回调
     *
     *
     */
    private class OverrideListener implements NotifyListener {

        /** 表示向注册中心订阅服务，例如：
         * provider://172.17.48.52:20880/com.alibaba.dubbo.demo.DemoService
         *         ?category=configurators
         *         &check=false
         *         &anyhost=true
         *         &application=demo-provider
         *         &dubbo=2.0.2
         *         &generic=false
         *         &interface=com.alibaba.dubbo.demo.DemoService
         *         &methods=sayHello
         */
        private final URL subscribeUrl;
        /** 服务提供者的Invoker对象 */
        private final Invoker originInvoker;

        public OverrideListener(URL subscribeUrl, Invoker originalInvoker) {
            this.subscribeUrl = subscribeUrl;
            this.originInvoker = originalInvoker;
        }

        /**
         * 入参对应/dubbo/com.alibaba.dubbo.demo.DemoService/configurators的所有子节点：例如，你在dubbo-admin的服务治理中将一个服务禁用，
         * 则这里入参就会对应一条url是：/dubbo/com.alibaba.dubbo.demo.DemoService/configurators/override://172.16.120.103:20880/com.alibaba.dubbo.demo.DemoService?category=configurators&disabled=true&dynamic=false&enabled=true
         *
         * @param urls 已注册信息的列表，始终不为空，含义与 {@link com.alibaba.dubbo.registry.RegistryService#lookup(URL)} 的返回值相同
         */
        public synchronized void notify(List<URL> urls) {

            // ===== 找出匹配当前服务的配置url =====
            logger.debug("original override urls: " + urls);
            List<URL> matchedUrls = getMatchedUrls(urls, subscribeUrl);
            logger.debug("subscribe url: " + subscribeUrl + ", override urls: " + matchedUrls);
            if (matchedUrls.isEmpty()) {
                return;
            }

            // 将url配置转为Configurator对象
            List<Configurator> configurators = RegistryDirectory.toConfigurators(matchedUrls);

            final Invoker<?> invoker;
            if (originInvoker instanceof InvokerDelegete) {
                invoker = ((InvokerDelegete<?>) originInvoker).getInvoker();
            } else {
                invoker = originInvoker;
            }
            // 获取这个Invoker对应服务提供者的服务配置信息
            URL originUrl = RegistryProtocol.this.getProviderUrl(invoker);
            String key = getCacheKey(originInvoker);
            ExporterChangeableWrapper<?> exporter = bounds.get(key);
            if (exporter == null) {
                logger.warn(new IllegalStateException("error state, exporter should not be null"));
                return;
            }
            // 目前，可能已经合并了很多次
            URL currentUrl = exporter.getInvoker().getUrl();
            // 将服务治理的规则配置，以参数的形式，追加到url中
            URL newUrl = getConfigedInvokerUrl(configurators, originUrl);
            if (!currentUrl.equals(newUrl)) {
                RegistryProtocol.this.doChangeLocalExport(originInvoker, newUrl);
                logger.info("exported provider url changed, origin url: " + originUrl + ", old export url: " + currentUrl + ", new export url: " + newUrl);
            }
        }
        private List<URL> getMatchedUrls(List<URL> configuratorUrls, URL currentSubscribe) {
            List<URL> result = new ArrayList<URL>();
            for (URL url : configuratorUrls) {
                URL overrideUrl = url;
                // 与旧版本兼容：如果category=null 并且是 override协议
                if (url.getParameter(Constants.CATEGORY_KEY) == null && Constants.OVERRIDE_PROTOCOL.equals(url.getProtocol())) {
                    overrideUrl = url.addParameter(Constants.CATEGORY_KEY, Constants.CONFIGURATORS_CATEGORY);
                }

                // 检查是否将URL应用于当前服务
                if (UrlUtils.isMatch(currentSubscribe, overrideUrl)) {
                    result.add(url);
                }
            }
            return result;
        }

        /**
         * 将服务治理的规则配置，以参数的形式，追加到url中
         *
         * @param configurators     服务治理新加的规则配置，比如：设置超时、禁用服务等
         * @param url               原始的服务元数据
         * @return
         */
        private URL getConfigedInvokerUrl(List<Configurator> configurators, URL url) {
            for (Configurator configurator : configurators) {
                url = configurator.configure(url);
            }
            return url;
        }
    }

    /**
     * exporter proxy, establish the corresponding relationship between the returned exporter and the exporter exported by the protocol,
     * and can modify the relationship at the time of override.
     *
     * 调用相应的协议暴露服务后，会将{@link Protocol#export(Invoker)}的放回值，{@link Exporter}包装为一个ExporterChangeableWrapper
     * 对象，然后缓存到{@link #bounds}
     * 具体代码，参见{@link #doLocalExport(Invoker)}方法
     *
     * @param <T>
     */
    private class ExporterChangeableWrapper<T> implements Exporter<T> {

        private final Invoker<T> originInvoker;
        private Exporter<T> exporter;

        public ExporterChangeableWrapper(Exporter<T> exporter, Invoker<T> originInvoker) {
            this.exporter = exporter;
            this.originInvoker = originInvoker;
        }

        public Invoker<T> getOriginInvoker() {
            return originInvoker;
        }
        public Invoker<T> getInvoker() {
            return exporter.getInvoker();
        }

        public void setExporter(Exporter<T> exporter) {
            this.exporter = exporter;
        }
        public void unexport() {
            String key = getCacheKey(this.originInvoker);
            bounds.remove(key);
            exporter.unexport();
        }
    }
}