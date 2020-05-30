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
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.cluster.Configurator;
import com.alibaba.dubbo.rpc.cluster.ConfiguratorFactory;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.cluster.RouterFactory;
import com.alibaba.dubbo.rpc.cluster.directory.AbstractDirectory;
import com.alibaba.dubbo.rpc.cluster.directory.StaticDirectory;
import com.alibaba.dubbo.rpc.cluster.support.ClusterUtils;
import com.alibaba.dubbo.rpc.protocol.InvokerWrapper;
import com.alibaba.dubbo.rpc.support.RpcUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RegistryDirectory：维护着所有可用的远程Invoker或者本地的Invoker
 * RegistryDirectory：是一种动态服务目录，实现了 NotifyListener 接口。当注册中心服务配置发生变化后，RegistryDirectory 可收到与当前服务相关的变化。
 * 收到变更通知后，RegistryDirectory 可根据配置变更信息刷新 Invoker 列表。
 * RegistryDirectory 中有几个比较重要的逻辑：
 * 第一是 Invoker 的列举逻辑；
 * 第二是 接收服务配置变更的逻辑；
 * 第三是 Invoker 列表的刷新逻辑。
 *
 */
public class RegistryDirectory<T> extends AbstractDirectory<T> implements NotifyListener {

    private static final Logger logger = LoggerFactory.getLogger(RegistryDirectory.class);

    /**
     * 将 Directory 中的多个 Invoker 伪装成一个 Invoker, 对上层透明，包含集群的容错机制，Cluster根据不同的集群策略创建不同的 Invoker；
     * 默认实现是FailOverCluster，当出现失败重试其他服务的策略
     */
    private static final Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();
    private static final RouterFactory routerFactory = ExtensionLoader.getExtensionLoader(RouterFactory.class).getAdaptiveExtension();
    private static final ConfiguratorFactory configuratorFactory = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class).getAdaptiveExtension();


    /** 例如：${group}/com.alibaba.dubbo.demo.DemoService:${version}，在构造器中初始化，不能为空*/
    private final String serviceKey;
    /** 服务接口类型，在构造器中初始化，不能为空*/
    private final Class<T> serviceType;
    /** 保存要获取的服务接口信息，在构造器中初始化，不能为空，例如：
     0 = {HashMap$Node@2525} "side" -> "consumer"
     1 = {HashMap$Node@2526} "application" -> "demo-consumer"
     2 = {HashMap$Node@2527} "register.ip" -> "30.6.28.128"
     3 = {HashMap$Node@2528} "methods" -> "sayHello"
     4 = {HashMap$Node@2529} "dubbo" -> "2.0.0"
     5 = {HashMap$Node@2530} "pid" -> "1230064"
     6 = {HashMap$Node@2531} "check" -> "false"
     7 = {HashMap$Node@2532} "interface" -> "com.alibaba.dubbo.demo.DemoService"
     8 = {HashMap$Node@2533} "timestamp" -> "1522228545805"
     * */
    private final Map<String, String> queryMap;
    /** 在构造时进行初始化，声明不为null，并且始终分配非null值 */
    private final URL directoryUrl;
    /** 表示服务接口的方法名 */
    private final String[] serviceMethods;
    private final boolean multiGroup;
    /** 表示服务所使用的协议 */
    private Protocol protocol;
    /** 对应<dubbo:registry address="multicast://224.5.6.7:1234"/>配置 */
    private Registry registry;
    /** 当所有可用的服务被禁用时，会通过监听方法将该值设置ture，{@link #refreshInvoker(List)} */
    private volatile boolean forbidden = false;
    /** 例如：multicast://224.5.6.7:1234/com.alibaba.dubbo.registry.RegistryService
     * ?application=demo-consumer
     * &check=false
     * &dubbo=2.0.0
     * &interface=com.alibaba.dubbo.demo.DemoService
     * &methods=sayHello
     * &pid=1230064
     * &register.ip=30.6.28.128
     * &side=consumer
     * &timestamp=1522228545805
     */
    private volatile URL overrideDirectoryUrl;
    /**
     * override rules
     * Priority: override>-D>consumer>provider
     * Rule one: for a certain provider <ip:port,timeout=100>
     * Rule two: for all providers <* ,timeout=5000>
     *
     * The initial value is null and the midway may be assigned to null, please use the local variable reference
     */
    private volatile List<Configurator> configurators;
    /** Map<url, Invoker> 缓存服务接口到Invoker的映射关系 */
    private volatile Map<String, Invoker<T>> urlInvokerMap;
    /** Map<methodName, Invoker> 缓存服务方法到Invoker的映射关系 */
    private volatile Map<String, List<Invoker<T>>> methodInvokerMap;
    /**
     * Set<invokerUrls> cache invokeUrls to invokers mapping.
     * The initial value is null and the midway may be assigned to null, please use the local variable reference
     */
    private volatile Set<URL> cachedInvokerUrls;


    /**
     * url入参例如：
     * zookeeper://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService
     * ?application=demo-consumer
     * &dubbo=2.0.0
     * &pid=37643
     * &refer=application=demo-consumer
     * &check=false
     * &dubbo=2.0.0
     * &interface=com.alibaba.dubbo.demo.DemoService
     * &methods=sayHello
     * &pid=37643
     * &register.ip=192.168.1.101
     * &side=consumer
     * &stub=whz.stub.DemoServiceStub
     * &timestamp=1589684990032
     * &timestamp=1589684990057
     *
     * @param serviceType
     * @param url
     */
    public RegistryDirectory(Class<T> serviceType, URL url) {
        super(url);
        if (serviceType == null)
            throw new IllegalArgumentException("service type is null.");
        if (url.getServiceKey() == null || url.getServiceKey().length() == 0)
            throw new IllegalArgumentException("registry serviceKey is null.");
        this.serviceType = serviceType;
        // 例如：${group}/com.alibaba.dubbo.demo.DemoService:${version}
        this.serviceKey = url.getServiceKey();
        // {
        // side=consumer,
        // application=demo-consumer,
        // register.ip=192.168.1.101,
        // methods=sayHello,
        // stub=whz.stub.DemoServiceStub,
        // dubbo=2.0.0,
        // pid=37655,
        // check=false,
        // interface=com.alibaba.dubbo.demo.DemoService,
        // timestamp=1589685557110
        // }
        this.queryMap = StringUtils.parseQueryString(url.getParameterAndDecoded(Constants.REFER_KEY));
        this.overrideDirectoryUrl = this.directoryUrl = url.setPath(url.getServiceInterface()).clearParameters().addParameters(queryMap).removeParameter(Constants.MONITOR_KEY);
        // 获取group配置
        String group = directoryUrl.getParameter(Constants.GROUP_KEY, "");
        this.multiGroup = group != null && ("*".equals(group) || group.contains(","));
        String methods = queryMap.get(Constants.METHODS_KEY);
        this.serviceMethods = methods == null ? null : Constants.COMMA_SPLIT_PATTERN.split(methods);
    }









    /**
     * 根据方法调用信息，查找所有可以调用的Invoker对象
     * @param invocation
     * @return
     */
    public List<Invoker<T>> doList(Invocation invocation) {
        // 服务提供者关闭或禁用了服务，此时抛出 No provider 异常
        // 检查服务是否被禁用，如果配置中心禁用了某个服务，则该服务无法被调用。如果服务被禁用则会抛出异常
        if (forbidden) {
            throw new RpcException(RpcException.FORBIDDEN_EXCEPTION,
                    "No provider available from registry " + getUrl().getAddress() + " for service " + getConsumerUrl().getServiceKey() + " on consumer " +  NetUtils.getLocalHost()
                            + " use dubbo version " + Version.getVersion() + ", may be providers disabled or not registered ?");
        }

        List<Invoker<T>> invokers = null;
        // 获取 Invoker 本地缓存
        Map<String, List<Invoker<T>>> localMethodInvokerMap = this.methodInvokerMap;
        if (localMethodInvokerMap != null && localMethodInvokerMap.size() > 0) {
            // 获取方法名和参数列表
            String methodName = RpcUtils.getMethodName(invocation);
            Object[] args = RpcUtils.getArguments(invocation);
            // 检测参数列表的第一个参数是否为 String 或 enum 类型
            if (args != null && args.length > 0 && args[0] != null
                    && (args[0] instanceof String || args[0].getClass().isEnum())) {
                // 通过 方法名 + 第一个参数名称 查询 Invoker 列表，具体的使用场景暂时没想到
                invokers = localMethodInvokerMap.get(methodName + "." + args[0]);
            }

            if (invokers == null) {
                // 通过方法名获取 Invoker 列表
                invokers = localMethodInvokerMap.get(methodName);
            }

            if (invokers == null) {
                // 通过星号 * 获取 Invoker 列表
                invokers = localMethodInvokerMap.get(Constants.ANY_VALUE);
            }

            // 冗余逻辑，pull request #2861 移除了下面的 if 分支代码
            if (invokers == null) {
                Iterator<List<Invoker<T>>> iterator = localMethodInvokerMap.values().iterator();
                if (iterator.hasNext()) {
                    invokers = iterator.next();
                }
            }
        }

        // 返回 Invoker 列表
        return invokers == null ? new ArrayList<Invoker<T>>(0) : invokers;
    }
    public Class<T> getInterface() {
        return serviceType;
    }
    public URL getUrl() {
        return this.overrideDirectoryUrl;
    }
    public boolean isAvailable() {
        if (isDestroyed()) {
            return false;
        }
        Map<String, Invoker<T>> localUrlInvokerMap = urlInvokerMap;
        if (localUrlInvokerMap != null && localUrlInvokerMap.size() > 0) {
            for (Invoker<T> invoker : new ArrayList<Invoker<T>>(localUrlInvokerMap.values())) {
                if (invoker.isAvailable()) {
                    return true;
                }
            }
        }
        return false;
    }
    /** Haomin: added for test purpose */
    public Map<String, Invoker<T>> getUrlInvokerMap() {
        return urlInvokerMap;
    }
    /** Haomin: added for test purpose */
    public Map<String, List<Invoker<T>>> getMethodInvokerMap() {
        return methodInvokerMap;
    }










    /**
     * RegistryDirectory 是一个动态服务目录，会随注册中心配置的变化进行动态调整。因此 RegistryDirectory 实现了 NotifyListener 接口，通过这个接口获取注册中心变更通知。
     *
     * @param urls 已注册信息列表，总不为空，含义同{@link com.alibaba.dubbo.registry.RegistryService#lookup(URL)}的返回值。
     *              一个服务可能存在多个服务提供者，只要有一个服务发生变化，就会触发该方法通知服务订阅者，入参是一个List，是因为
     */
    public synchronized void notify(List<URL> urls) {

        // ====== 定义三个集合，分别用于存放服务提供者 url，路由 url，配置器 url =====
        List<URL> invokerUrls = new ArrayList<URL>();
        List<URL> routerUrls = new ArrayList<URL>();
        List<URL> configuratorUrls = new ArrayList<URL>();
        for (URL url : urls) {
            String protocol = url.getProtocol();
            String category = url.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
            // routers
            if (Constants.ROUTERS_CATEGORY.equals(category) || Constants.ROUTE_PROTOCOL.equals(protocol)) {
                routerUrls.add(url);
            }
            // configurators
            else if (Constants.CONFIGURATORS_CATEGORY.equals(category) || Constants.OVERRIDE_PROTOCOL.equals(protocol)) {
                configuratorUrls.add(url);
            }
            // provider
            else if (Constants.PROVIDERS_CATEGORY.equals(category)) {
                invokerUrls.add(url);
            } else {
                logger.warn("Unsupported category " + category + " in notified url: " + url + " from registry " + getUrl().getAddress() + " to consumer " + NetUtils.getLocalHost());
            }
        }

        // 1、configurators
        if (configuratorUrls != null && configuratorUrls.size() > 0) {
            // 将 url 转成 Configurator
            this.configurators = toConfigurators(configuratorUrls);
        }

        // 2、routers
        if (routerUrls != null && routerUrls.size() > 0) {
            // 将 url 转成 Router
            List<Router> routers = toRouters(routerUrls);
            if (routers != null) {
                setRouters(routers);
            }
        }

        // 本地配置
        List<Configurator> localConfigurators = this.configurators;
        // 合并或替代变更的配置参数
        this.overrideDirectoryUrl = directoryUrl;
        if (localConfigurators != null && localConfigurators.size() > 0) {
            for (Configurator configurator : localConfigurators) {
                // 配置 overrideDirectoryUrl
                this.overrideDirectoryUrl = configurator.configure(overrideDirectoryUrl);
            }
        }

        // 刷新 Invoker 列表
        refreshInvoker(invokerUrls);
    }
    /**
     * 将替代网址转换为映射，以便在重新引用时使用。 每次发送所有规则，网址将重新组合并计算
     *
     * @param urls Contract:
     *             </br>1.override://0.0.0.0/...( or override://ip:port...?anyhost=true)&para1=value1... 表示全局规则（所有提供者均生效）
     *             </br>2.override://ip:port...?anyhost=false 特殊规则（仅适用于特定providers）
     *             </br>3.override:// 不支持规则...，需要由注册表本身进行计算。
     *             </br>4.override://0.0.0.0/ 没有参数意味着覆盖
     * @return
     */
    public static List<Configurator> toConfigurators(List<URL> urls) {
        if (urls == null || urls.size() == 0) {
            return Collections.emptyList();
        }

        List<Configurator> configurators = new ArrayList<Configurator>(urls.size());
        for (URL url : urls) {
            if (Constants.EMPTY_PROTOCOL.equals(url.getProtocol())) {
                configurators.clear();
                break;
            }

            Map<String, String> override = new HashMap<String, String>(url.getParameters());
            //The anyhost parameter of override may be added automatically, it can't change the judgement of changing url
            override.remove(Constants.ANYHOST_KEY);
            if (override.size() == 0) {
                configurators.clear();
                continue;
            }
            configurators.add(configuratorFactory.getConfigurator(url));
        }
        Collections.sort(configurators);
        return configurators;
    }
    /**
     * @param urls
     * @return null : no routers ,do nothing
     * else :routers list
     */
    private List<Router> toRouters(List<URL> urls) {
        List<Router> routers = new ArrayList<Router>();
        if (urls == null || urls.size() < 1) {
            return routers;
        }
        if (urls != null && urls.size() > 0) {
            for (URL url : urls) {
                if (Constants.EMPTY_PROTOCOL.equals(url.getProtocol())) {
                    continue;
                }
                String routerType = url.getParameter(Constants.ROUTER_KEY);
                if (routerType != null && routerType.length() > 0) {
                    url = url.setProtocol(routerType);
                }
                try {
                    Router router = routerFactory.getRouter(url);
                    if (!routers.contains(router))
                        routers.add(router);
                } catch (Throwable t) {
                    logger.error("convert router url to router error, url: " + url, t);
                }
            }
        }
        return routers;
    }
    /**
     * Convert the invokerURL list to the Invoker Map. The rules of the conversion are as follows:
     * 1.If URL has been converted to invoker, it is no longer re-referenced and obtained directly from the cache, and notice that any parameter changes in the URL will be re-referenced.
     * 2.If the incoming invoker list is not empty, it means that it is the latest invoker list
     * 3.If the list of incoming invokerUrl is empty, It means that the rule is only a override rule or a route rule, which needs to be re-contrasted to decide whether to re-reference.
     *
     * @param invokerUrls this parameter can't be null
     */
    // TODO: 2017/8/31 FIXME The thread pool should be used to refresh the address, otherwise the task may be accumulated.
    private void refreshInvoker(List<URL> invokerUrls) {
        // ======= invokerUrls 仅有一个元素，且 url 协议头为 empty，此时表示禁用所有服务 =======
        if (invokerUrls != null && invokerUrls.size() == 1 && invokerUrls.get(0) != null && Constants.EMPTY_PROTOCOL.equals(invokerUrls.get(0).getProtocol())) {
            // 设置 forbidden 为 true
            this.forbidden = true;
            this.methodInvokerMap = null;
            // 销毁所有 Invoker
            destroyAllInvokers();
        }



        else {
            this.forbidden = false;
            Map<String, Invoker<T>> oldUrlInvokerMap = this.urlInvokerMap;
            if (invokerUrls.size() == 0 && this.cachedInvokerUrls != null) {
                // 添加缓存 url 到 invokerUrls 中
                invokerUrls.addAll(this.cachedInvokerUrls);
            } else {
                this.cachedInvokerUrls = new HashSet<URL>();
                // 缓存 invokerUrls
                this.cachedInvokerUrls.addAll(invokerUrls);
            }
            if (invokerUrls.size() == 0) {
                return;
            }

            // 将 url 转成 Invoker
            Map<String, Invoker<T>> newUrlInvokerMap = toInvokers(invokerUrls);
            // 将 newUrlInvokerMap 转成方法名到 Invoker 列表的映射
            Map<String, List<Invoker<T>>> newMethodInvokerMap = toMethodInvokers(newUrlInvokerMap);
            // 转换出错，直接打印异常，并返回
            if (newUrlInvokerMap == null || newUrlInvokerMap.size() == 0) {
                logger.error(new IllegalStateException("urls to invokers error .invokerUrls.size :" + invokerUrls.size() + ", invoker.size :0. urls :" + invokerUrls.toString()));
                return;
            }

            // 合并多个组的 Invoker
            this.methodInvokerMap = multiGroup ? toMergeMethodInvokerMap(newMethodInvokerMap) : newMethodInvokerMap;
            this.urlInvokerMap = newUrlInvokerMap;
            try {
                // 销毁无用 Invoker
                destroyUnusedInvokers(oldUrlInvokerMap, newUrlInvokerMap);
            } catch (Exception e) {
                logger.warn("destroyUnusedInvokers error. ", e);
            }
        }
    }
    /**
     * Turn urls into invokers, and if url has been refer, will not re-reference.
     *
     * @param urls
     * @return invokers
     */
    private Map<String, Invoker<T>> toInvokers(List<URL> urls) {
        Map<String, Invoker<T>> newUrlInvokerMap = new HashMap<String, Invoker<T>>();
        if (urls == null || urls.size() == 0) {
            return newUrlInvokerMap;
        }
        Set<String> keys = new HashSet<String>();
        String queryProtocols = this.queryMap.get(Constants.PROTOCOL_KEY);
        for (URL providerUrl : urls) {
            // If protocol is configured at the reference side, only the matching protocol is selected
            if (queryProtocols != null && queryProtocols.length() > 0) {
                boolean accept = false;
                String[] acceptProtocols = queryProtocols.split(",");
                for (String acceptProtocol : acceptProtocols) {
                    if (providerUrl.getProtocol().equals(acceptProtocol)) {
                        accept = true;
                        break;
                    }
                }
                if (!accept) {
                    continue;
                }
            }
            if (Constants.EMPTY_PROTOCOL.equals(providerUrl.getProtocol())) {
                continue;
            }
            if (!ExtensionLoader.getExtensionLoader(Protocol.class).hasExtension(providerUrl.getProtocol())) {
                logger.error(new IllegalStateException("Unsupported protocol " + providerUrl.getProtocol() + " in notified url: " + providerUrl + " from registry " + getUrl().getAddress() + " to consumer " + NetUtils.getLocalHost()
                        + ", supported protocol: " + ExtensionLoader.getExtensionLoader(Protocol.class).getSupportedExtensions()));
                continue;
            }
            URL url = mergeUrl(providerUrl);

            String key = url.toFullString(); // The parameter urls are sorted
            if (keys.contains(key)) { // Repeated url
                continue;
            }
            keys.add(key);
            // Cache key is url that does not merge with consumer side parameters, regardless of how the consumer combines parameters, if the server url changes, then refer again
            Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap; // local reference
            Invoker<T> invoker = localUrlInvokerMap == null ? null : localUrlInvokerMap.get(key);
            if (invoker == null) { // Not in the cache, refer again
                try {
                    boolean enabled = true;
                    if (url.hasParameter(Constants.DISABLED_KEY)) {
                        enabled = !url.getParameter(Constants.DISABLED_KEY, false);
                    } else {
                        enabled = url.getParameter(Constants.ENABLED_KEY, true);
                    }
                    if (enabled) {
                        invoker = new InvokerDelegate<T>(protocol.refer(serviceType, url), url, providerUrl);
                    }
                } catch (Throwable t) {
                    logger.error("Failed to refer invoker for interface:" + serviceType + ",url:(" + url + ")" + t.getMessage(), t);
                }
                if (invoker != null) { // Put new invoker in cache
                    newUrlInvokerMap.put(key, invoker);
                }
            } else {
                newUrlInvokerMap.put(key, invoker);
            }
        }
        keys.clear();
        return newUrlInvokerMap;
    }
    /**
     * Transform the invokers list into a mapping relationship with a method
     *
     * @param invokersMap Invoker Map
     * @return Mapping relation between Invoker and method
     */
    private Map<String, List<Invoker<T>>> toMethodInvokers(Map<String, Invoker<T>> invokersMap) {
        Map<String, List<Invoker<T>>> newMethodInvokerMap = new HashMap<String, List<Invoker<T>>>();
        // According to the methods classification declared by the provider URL, the methods is compatible with the registry to execute the filtered methods
        List<Invoker<T>> invokersList = new ArrayList<Invoker<T>>();
        if (invokersMap != null && invokersMap.size() > 0) {
            for (Invoker<T> invoker : invokersMap.values()) {
                String parameter = invoker.getUrl().getParameter(Constants.METHODS_KEY);
                if (parameter != null && parameter.length() > 0) {
                    String[] methods = Constants.COMMA_SPLIT_PATTERN.split(parameter);
                    if (methods != null && methods.length > 0) {
                        for (String method : methods) {
                            if (method != null && method.length() > 0
                                    && !Constants.ANY_VALUE.equals(method)) {
                                List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
                                if (methodInvokers == null) {
                                    methodInvokers = new ArrayList<Invoker<T>>();
                                    newMethodInvokerMap.put(method, methodInvokers);
                                }
                                methodInvokers.add(invoker);
                            }
                        }
                    }
                }
                invokersList.add(invoker);
            }
        }
        List<Invoker<T>> newInvokersList = route(invokersList, null);
        newMethodInvokerMap.put(Constants.ANY_VALUE, newInvokersList);
        if (serviceMethods != null && serviceMethods.length > 0) {
            for (String method : serviceMethods) {
                List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
                if (methodInvokers == null || methodInvokers.size() == 0) {
                    methodInvokers = newInvokersList;
                }
                newMethodInvokerMap.put(method, route(methodInvokers, method));
            }
        }
        // sort and unmodifiable
        for (String method : new HashSet<String>(newMethodInvokerMap.keySet())) {
            List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
            Collections.sort(methodInvokers, InvokerComparator.getComparator());
            newMethodInvokerMap.put(method, Collections.unmodifiableList(methodInvokers));
        }
        return Collections.unmodifiableMap(newMethodInvokerMap);
    }
    private Map<String, List<Invoker<T>>> toMergeMethodInvokerMap(Map<String, List<Invoker<T>>> methodMap) {
        Map<String, List<Invoker<T>>> result = new HashMap<String, List<Invoker<T>>>();
        for (Map.Entry<String, List<Invoker<T>>> entry : methodMap.entrySet()) {
            String method = entry.getKey();
            List<Invoker<T>> invokers = entry.getValue();
            Map<String, List<Invoker<T>>> groupMap = new HashMap<String, List<Invoker<T>>>();
            for (Invoker<T> invoker : invokers) {
                String group = invoker.getUrl().getParameter(Constants.GROUP_KEY, "");
                List<Invoker<T>> groupInvokers = groupMap.get(group);
                if (groupInvokers == null) {
                    groupInvokers = new ArrayList<Invoker<T>>();
                    groupMap.put(group, groupInvokers);
                }
                groupInvokers.add(invoker);
            }
            if (groupMap.size() == 1) {
                result.put(method, groupMap.values().iterator().next());
            } else if (groupMap.size() > 1) {
                List<Invoker<T>> groupInvokers = new ArrayList<Invoker<T>>();
                for (List<Invoker<T>> groupList : groupMap.values()) {
                    groupInvokers.add(cluster.join(new StaticDirectory<T>(groupList)));
                }
                result.put(method, groupInvokers);
            } else {
                result.put(method, invokers);
            }
        }
        return result;
    }
    /**
     * Merge url parameters. the order is: override > -D >Consumer > Provider
     *
     * @param providerUrl
     * @return
     */
    private URL mergeUrl(URL providerUrl) {
        providerUrl = ClusterUtils.mergeUrl(providerUrl, queryMap); // Merge the consumer side parameters

        List<Configurator> localConfigurators = this.configurators; // local reference
        if (localConfigurators != null && localConfigurators.size() > 0) {
            for (Configurator configurator : localConfigurators) {
                providerUrl = configurator.configure(providerUrl);
            }
        }

        providerUrl = providerUrl.addParameter(Constants.CHECK_KEY, String.valueOf(false)); // Do not check whether the connection is successful or not, always create Invoker!

        // The combination of directoryUrl and override is at the end of notify, which can't be handled here
        this.overrideDirectoryUrl = this.overrideDirectoryUrl.addParametersIfAbsent(providerUrl.getParameters()); // Merge the provider side parameters

        if ((providerUrl.getPath() == null || providerUrl.getPath().length() == 0)
                && "dubbo".equals(providerUrl.getProtocol())) { // Compatible version 1.0
            //fix by tony.chenl DUBBO-44
            String path = directoryUrl.getParameter(Constants.INTERFACE_KEY);
            if (path != null) {
                int i = path.indexOf('/');
                if (i >= 0) {
                    path = path.substring(i + 1);
                }
                i = path.lastIndexOf(':');
                if (i >= 0) {
                    path = path.substring(0, i);
                }
                providerUrl = providerUrl.setPath(path);
            }
        }
        return providerUrl;
    }
    private List<Invoker<T>> route(List<Invoker<T>> invokers, String method) {
        Invocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
        List<Router> routers = getRouters();
        if (routers != null) {
            for (Router router : routers) {
                if (router.getUrl() != null) {
                    invokers = router.route(invokers, getConsumerUrl(), invocation);
                }
            }
        }
        return invokers;
    }
    /** Close all invokers */
    private void destroyAllInvokers() {
        Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap; // local reference
        if (localUrlInvokerMap != null) {
            for (Invoker<T> invoker : new ArrayList<Invoker<T>>(localUrlInvokerMap.values())) {
                try {
                    invoker.destroy();
                } catch (Throwable t) {
                    logger.warn("Failed to destroy service " + serviceKey + " to provider " + invoker.getUrl(), t);
                }
            }
            localUrlInvokerMap.clear();
        }
        methodInvokerMap = null;
    }
    /**
     * Check whether the invoker in the cache needs to be destroyed
     * If set attribute of url: refer.autodestroy=false, the invokers will only increase without decreasing,there may be a refer leak
     *
     * @param oldUrlInvokerMap
     * @param newUrlInvokerMap
     */
    private void destroyUnusedInvokers(Map<String, Invoker<T>> oldUrlInvokerMap, Map<String, Invoker<T>> newUrlInvokerMap) {
        if (newUrlInvokerMap == null || newUrlInvokerMap.size() == 0) {
            destroyAllInvokers();
            return;
        }
        // check deleted invoker
        List<String> deleted = null;
        if (oldUrlInvokerMap != null) {
            Collection<Invoker<T>> newInvokers = newUrlInvokerMap.values();
            for (Map.Entry<String, Invoker<T>> entry : oldUrlInvokerMap.entrySet()) {
                if (!newInvokers.contains(entry.getValue())) {
                    if (deleted == null) {
                        deleted = new ArrayList<String>();
                    }
                    deleted.add(entry.getKey());
                }
            }
        }

        if (deleted != null) {
            for (String url : deleted) {
                if (url != null) {
                    Invoker<T> invoker = oldUrlInvokerMap.remove(url);
                    if (invoker != null) {
                        try {
                            invoker.destroy();
                            if (logger.isDebugEnabled()) {
                                logger.debug("destory invoker[" + invoker.getUrl() + "] success. ");
                            }
                        } catch (Exception e) {
                            logger.warn("destory invoker[" + invoker.getUrl() + "] faild. " + e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }


    /**
     * 服务导出到注册中后，会调用该方法订阅服务节点，参见：{@link RegistryProtocol#doRefer(Cluster, Registry, Class, URL)}
     *
     * @param url
     */
    public void subscribe(URL url) {
        setConsumerUrl(url);
        registry.subscribe(url, this);
    }

    public void destroy() {
        if (isDestroyed()) {
            return;
        }
        // unsubscribe.
        try {
            if (getConsumerUrl() != null && registry != null && registry.isAvailable()) {
                registry.unsubscribe(getConsumerUrl(), this);
            }
        } catch (Throwable t) {
            logger.warn("unexpeced error when unsubscribe service " + serviceKey + "from registry" + registry.getUrl(), t);
        }
        super.destroy(); // must be executed after unsubscribing
        try {
            destroyAllInvokers();
        } catch (Throwable t) {
            logger.warn("Failed to destroy service " + serviceKey, t);
        }
    }










    // setter ...

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }
    public void setRegistry(Registry registry) {
        this.registry = registry;
    }





    private static class InvokerComparator implements Comparator<Invoker<?>> {

        private static final InvokerComparator comparator = new InvokerComparator();

        private InvokerComparator() {
        }

        public static InvokerComparator getComparator() {
            return comparator;
        }

        public int compare(Invoker<?> o1, Invoker<?> o2) {
            return o1.getUrl().toString().compareTo(o2.getUrl().toString());
        }

    }

    /**
     * The delegate class, which is mainly used to store the URL address sent by the registry,and can be reassembled on the basis of providerURL queryMap overrideMap for re-refer.
     *
     * @param <T>
     */
    private static class InvokerDelegate<T> extends InvokerWrapper<T> {
        private URL providerUrl;

        public InvokerDelegate(Invoker<T> invoker, URL url, URL providerUrl) {
            super(invoker, url);
            this.providerUrl = providerUrl;
        }

        public URL getProviderUrl() {
            return providerUrl;
        }
    }
}
