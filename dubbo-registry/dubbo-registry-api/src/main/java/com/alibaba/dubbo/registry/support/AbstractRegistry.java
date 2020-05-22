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
package com.alibaba.dubbo.registry.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AbstractRegistry. (SPI, Prototype, ThreadSafe)
 * 该抽象注册服务主要关注注册中心的本地缓存文件和加载文件到内存：
 * 缓存的存在就是用空间换取时间，如果每次远程调用都要先从注册中心获取一次可调用服务列表，则会让注册中心承受巨大的流程压力。另外，每次额外的
 * 网络请求也会让整个系统的性能下降。因此Dubbo的注册中心实现了通用的缓存机制，在抽象类AbstractRegistry实现。
 *
 */
public abstract class AbstractRegistry implements Registry {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /** 解析完缓存文件，将URL数据放到{@link #properties}时的用的分割符 */
    private static final char URL_SEPARATOR = ' ';
    /**
     * URL address separated regular expression for parsing the service provider URL list in the file cache
     * 解析缓存文件时，URL串的分割符：\\s：表示空格,回车,换行等空白符，+号表示一个或多个的意思
     */
    private static final String URL_SPLIT = "\\s+";

    /** 创建注册中心的URL，包括使用的实现中心实现，比如ZooKeeper，ip地址，端口等信息，该url信息创建一个注册中心实例 */
    private URL registryUrl;

    /** 表示注册中心的一个本地缓存文件 */
    private File file;
    /** 缓存的保存有同步和异步两种方式。异步使用线程池异步保存，如果线程在执行过程中出现异常，则会再次调用线程池不断重试，详见{@link #saveProperties} */
    private final boolean syncSaveFile;
    /**
     * 表示暴露服务的本地缓存数据，以键值对的形式记录注册中心的列表，而其他的是通知服务提供者的列表。通过properties.load(file)方法将缓
     * 存在硬盘的数据加载到内存，Properties<URL#serviceKey()，URL>
     *
     * properties保存了所有服务提供者的URL，使用URL#serviceKey()作为key，提供者列表、路由规则列表、配置规则列表等作为value。
     * 由于value是列表，当存在多个的时候使用空格隔开。还有一个特殊的key.registies，保存所有的注册中心的地址。如果应用在启动过程中，注册
     * 中心无法连接或者宕机，则Dubbo框架会自动通过本地缓存加载Invokers。
     * 例如：
     * {
     * com.alibaba.dubbo.demo.DemoService=empty://172.16.120.167:20880/com.alibaba.dubbo.demo.DemoService?anyhost=true&application=demo-provider&category=configurators&check=false&dubbo=2.0.0&generic=false&interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello&pid=3497&side=provider&timeout=1000&timestamp=1590047231690,
     * com.alibaba.dubbo.demo.HelloService=empty://172.16.120.167:20880/com.alibaba.dubbo.demo.HelloService?anyhost=true&application=demo-provider&category=configurators&check=false&dubbo=2.0.0&generic=false&interface=com.alibaba.dubbo.demo.HelloService&methods=sayHello&pid=3497&side=provider&timestamp=1590047293396
     * }
     */
    private final Properties properties = new Properties();

    /** 当{@link #syncSaveFile}为false时，使用异步的方式同步到缓存文件，通过该ExecutorService来实现异步加载 */
    private final ExecutorService registryCacheExecutor = Executors.newFixedThreadPool(1, new NamedThreadFactory("DubboSaveRegistryCache", true));

    /** 表示缓存最后一次改变的时间 */
    private final AtomicLong lastCacheChanged = new AtomicLong();
    /** 保存向注册中心注册的URL */
    private final Set<URL> registered = new ConcurrentHashSet<URL>();
    /** 保存注册中心URL对应的监听器，当注册中心数据发生变化时，通过该监听器通知客户端 */
    private final ConcurrentMap<URL, Set<NotifyListener>> subscribed = new ConcurrentHashMap<URL, Set<NotifyListener>>();
    /** 外层key是消费者的URL，内存Map的key是分类，包含providers、routers、consumers、configurators四种。value则是对应的服务列表，对于没有服务提供者的URL，它会以特色的empty://前缀开头 */
    private final ConcurrentMap<URL, Map<String, List<URL>>> notified = new ConcurrentHashMap<URL, Map<String, List<URL>>>();

    /** 用于标识注册中心是否已经销毁 */
    private AtomicBoolean destroyed = new AtomicBoolean(false);



    // 构造器

    public AbstractRegistry(URL url) {
        // 这里url为创建注册中心的url
        setUrl(url);
        // 启动文件保存计时器
        syncSaveFile = url.getParameter(Constants.REGISTRY_FILESAVE_SYNC_KEY, false);

        // 表示缓存在本地缓存文件，比如：C:\Users\whz/.dubbo/dubbo-registry-demo-provider-224.5.6.7:1234.cache
        String filename = url.getParameter(Constants.FILE_KEY, System.getProperty("user.home") + "/.dubbo/dubbo-registry-" + url.getParameter(Constants.APPLICATION_KEY) + "-" + url.getAddress() + ".cache");
        // 创建这个缓存文件
        File file = null;
        if (ConfigUtils.isNotEmpty(filename)) {
            file = new File(filename);
            if (!file.exists() && file.getParentFile() != null && !file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    throw new IllegalArgumentException("Invalid registry store file " + file + ", cause: Failed to create directory " + file.getParentFile() + "!");
                }
            }
        }
        this.file = file;

        // 加载该应用暴露服务的本地缓存数据：将缓存文件里的属性配置到properties
        loadProperties();
        // url.getBackupUrls()例如：zookeeper://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService?application=demo-provider&dubbo=2.0.0&interface=com.alibaba.dubbo.registry.RegistryService&pid=4959&timestamp=1590063820982
        notify(url.getBackupUrls());
    }
    /**
     * 读取缓存文件{@link #file}的信息，并保存到{@link #properties}
     */
    private void loadProperties() {
        if (file != null && file.exists()) {
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                properties.load(in);
                if (logger.isInfoEnabled()) {
                    logger.info("Load registry store file " + file + ", data: " + properties);
                }
            } catch (Throwable e) {
                logger.warn("Failed to load registry store file " + file, e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        }
    }
    protected void notify(List<URL> urls) {
        if (urls == null || urls.isEmpty()) return;

        for (Map.Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            // 被监听的URL
            URL url = entry.getKey();

            // 判断是否为我们要监听的URL
            if (!UrlUtils.isMatch(url, urls.get(0))) {
                continue;
            }

            // 获取监听器，一个URL可能对应多个监听器
            Set<NotifyListener> listeners = entry.getValue();
            if (listeners != null) {
                for (NotifyListener listener : listeners) {
                    try {
                        notify(url, listener, filterEmpty(url, urls));
                    } catch (Throwable t) {
                        logger.error("Failed to notify registry event, urls: " + urls + ", cause: " + t.getMessage(), t);
                    }
                }
            }
        }

    }
    protected static List<URL> filterEmpty(URL url, List<URL> urls) {
        if (urls == null || urls.size() == 0) {
            List<URL> result = new ArrayList<URL>(1);
            result.add(url.setProtocol(Constants.EMPTY_PROTOCOL));
            return result;
        }
        return urls;
    }

    /**
     * zk节点变更时，调用该方法，刷新本地缓存：
     *
     * 注意，此处会根据URL中的category属性值获取具体的类别：providers、routers、consumers、configurators，然后拉取直接子节点的数据进行通知（notify）。
     *     如果是providers类别的数据，则订阅方会更新本地Directory管理的Invoker服务列表；
     *     如果是routers分类，则订阅方会更新本地路由规则列表；
     *     如果是configuators类别，则订阅方会更新或覆盖本地动态参数列表
     *
     * @param url           服务的url
     * @param listener      providers、routers、consumers、configurators这些节点的子节点监听器
     * @param urls          表示providers、routers、consumers、configurators这些节点下的子节点
     */
    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        if (url == null) {
            throw new IllegalArgumentException("notify url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }

        if ((urls == null || urls.size() == 0) && !Constants.ANY_VALUE.equals(url.getServiceInterface())) {
            logger.warn("Ignore empty notify urls for subscribe url " + url);
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Notify urls for subscribe url " + url + ", urls: " + urls);
        }

        // 映射类别（例如：providers、routers、consumers、configurators）对应的子url
        Map<String, List<URL>> result = new HashMap<String, List<URL>>();
        for (URL u : urls) {
            // 判断这个消费者发过来的url和这个提供者注册的u，是否为同一个服务，比如判断服务接口是一样，group、version等信息是否一样
            if (UrlUtils.isMatch(url, u)) {
                String category = u.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
                List<URL> categoryList = result.get(category);
                if (categoryList == null) {
                    categoryList = new ArrayList<URL>();
                    result.put(category, categoryList);
                }
                categoryList.add(u);
            }
        }
        if (result.size() == 0) {
            return;
        }


        Map<String, List<URL>> categoryNotified = notified.get(url);
        if (categoryNotified == null) {
            notified.putIfAbsent(url, new ConcurrentHashMap<String, List<URL>>());
            categoryNotified = notified.get(url);
        }
        for (Map.Entry<String, List<URL>> entry : result.entrySet()) {
            String category = entry.getKey();
            List<URL> categoryList = entry.getValue();
            categoryNotified.put(category, categoryList);
            // 将数保存到内存，并更新本地缓存文件
            saveProperties(url);
            // 触发监听器
            listener.notify(categoryList);
        }
    }

    /**
     * 将数保存到内存，并更新本地缓存文件
     *
     * @param url
     */
    private void saveProperties(URL url) {
        if (file == null) {
            return;
        }

        try {
            StringBuilder buf = new StringBuilder();
            Map<String, List<URL>> categoryNotified = notified.get(url);
            if (categoryNotified != null) {
                for (List<URL> us : categoryNotified.values()) {
                    for (URL u : us) {
                        if (buf.length() > 0) {
                            buf.append(URL_SEPARATOR);
                        }
                        buf.append(u.toFullString());
                    }
                }
            }
            properties.setProperty(url.getServiceKey(), buf.toString());



            // 将properties数据写入文件


            long version = lastCacheChanged.incrementAndGet();
            if (syncSaveFile) {
                // 同步保存
                doSaveProperties(version);
            } else {
                // 异步保存
                registryCacheExecutor.execute(new SaveProperties(version));
            }
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    /**
     * 将properties数据写入文件
     *
     * @param version   当前写入时间
     */
    public void doSaveProperties(long version) {
        // 如果当前版本<缓存最后修改时间，则不作处理
        if (version < lastCacheChanged.get()) {
            return;
        }

        if (file == null) {
            return;
        }

        // Save
        try {
            File lockfile = new File(file.getAbsolutePath() + ".lock");
            if (!lockfile.exists()) {
                lockfile.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(lockfile, "rw");
            try {
                FileChannel channel = raf.getChannel();
                try {
                    // 通过文件来加锁
                    FileLock lock = channel.tryLock();
                    if (lock == null) {
                        throw new IOException("Can not lock the registry cache file " + file.getAbsolutePath() + ", ignore and retry later, maybe multi java process use the file, please config: dubbo.registry.file=xxx.properties");
                    }

                    // 将数据写入文件
                    try {
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        FileOutputStream outputFile = new FileOutputStream(file);
                        try {
                            properties.store(outputFile, "Dubbo Registry Cache");
                        } finally {
                            outputFile.close();
                        }
                    } finally {
                        lock.release();
                    }

                } finally {
                    channel.close();
                }
            } finally {
                raf.close();
            }
        } catch (Throwable e) {
            if (version < lastCacheChanged.get()) {
                return;
            } else {
                registryCacheExecutor.execute(new SaveProperties(lastCacheChanged.incrementAndGet()));
            }
            logger.warn("Failed to save registry store file, cause: " + e.getMessage(), e);
        }
    }






    public List<URL> getCacheUrls(URL url) {
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (key != null && key.length() > 0 && key.equals(url.getServiceKey())
                    && (Character.isLetter(key.charAt(0)) || key.charAt(0) == '_')
                    && value != null && value.length() > 0) {
                String[] arr = value.trim().split(URL_SPLIT);
                List<URL> urls = new ArrayList<URL>();
                for (String u : arr) {
                    urls.add(URL.valueOf(u));
                }
                return urls;
            }
        }
        return null;
    }

    public List<URL> lookup(URL url) {
        List<URL> result = new ArrayList<URL>();
        Map<String, List<URL>> notifiedUrls = getNotified().get(url);
        if (notifiedUrls != null && notifiedUrls.size() > 0) {
            for (List<URL> urls : notifiedUrls.values()) {
                for (URL u : urls) {
                    if (!Constants.EMPTY_PROTOCOL.equals(u.getProtocol())) {
                        result.add(u);
                    }
                }
            }
        } else {
            final AtomicReference<List<URL>> reference = new AtomicReference<List<URL>>();
            NotifyListener listener = new NotifyListener() {
                public void notify(List<URL> urls) {
                    reference.set(urls);
                }
            };
            subscribe(url, listener); // Subscribe logic guarantees the first notify to return
            List<URL> urls = reference.get();
            if (urls != null && urls.size() > 0) {
                for (URL u : urls) {
                    if (!Constants.EMPTY_PROTOCOL.equals(u.getProtocol())) {
                        result.add(u);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 本地保存注册的url
     *
     * @param url 注册信息，不允许为空，如：dubbo://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     */
    public void register(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("register url == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Register: " + url);
        }
        registered.add(url);
    }

    /**
     * 从注册中心移除URL
     * @param url 注册信息，不允许为空，如：dubbo://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     */
    public void unregister(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("unregister url == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Unregister: " + url);
        }
        registered.remove(url);
    }

    /**
     * 向注册中心注册监听器
     *
     * @param url       表示要监听的URL
     * @param listener  变更事件监听器，不允许为空
     */
    public void subscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("subscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("subscribe listener == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Subscribe: " + url);
        }

        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners == null) {
            subscribed.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
            listeners = subscribed.get(url);
        }
        listeners.add(listener);
    }

    /**
     * 从注册中心移除监听器
     * @param url       表示要监听的URL
     * @param listener  变更事件监听器，不允许为空
     */
    public void unsubscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("unsubscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("unsubscribe listener == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Unsubscribe: " + url);
        }
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    protected void recover() throws Exception {
        // register
        Set<URL> recoverRegistered = new HashSet<URL>(getRegistered());
        if (!recoverRegistered.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover register url " + recoverRegistered);
            }
            for (URL url : recoverRegistered) {
                register(url);
            }
        }
        // subscribe
        Map<URL, Set<NotifyListener>> recoverSubscribed = new HashMap<URL, Set<NotifyListener>>(getSubscribed());
        if (!recoverSubscribed.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover subscribe url " + recoverSubscribed.keySet());
            }
            for (Map.Entry<URL, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    subscribe(url, listener);
                }
            }
        }
    }

    public void destroy() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }

        if (logger.isInfoEnabled()) {
            logger.info("Destroy registry:" + getUrl());
        }
        Set<URL> destroyRegistered = new HashSet<URL>(getRegistered());
        if (!destroyRegistered.isEmpty()) {
            for (URL url : new HashSet<URL>(getRegistered())) {
                if (url.getParameter(Constants.DYNAMIC_KEY, true)) {
                    try {
                        unregister(url);
                        if (logger.isInfoEnabled()) {
                            logger.info("Destroy unregister url " + url);
                        }
                    } catch (Throwable t) {
                        logger.warn("Failed to unregister url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
        Map<URL, Set<NotifyListener>> destroySubscribed = new HashMap<URL, Set<NotifyListener>>(getSubscribed());
        if (!destroySubscribed.isEmpty()) {
            for (Map.Entry<URL, Set<NotifyListener>> entry : destroySubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    try {
                        unsubscribe(url, listener);
                        if (logger.isInfoEnabled()) {
                            logger.info("Destroy unsubscribe url " + url);
                        }
                    } catch (Throwable t) {
                        logger.warn("Failed to unsubscribe url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
    }



    // ====== getter and setter ... =======

    public URL getUrl() {
        return registryUrl;
    }
    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("registry url == null");
        }
        this.registryUrl = url;
    }
    public Set<URL> getRegistered() {
        return registered;
    }
    public Map<URL, Set<NotifyListener>> getSubscribed() {
        return subscribed;
    }
    public Map<URL, Map<String, List<URL>>> getNotified() {
        return notified;
    }
    public File getCacheFile() {
        return file;
    }
    public Properties getCacheProperties() {
        return properties;
    }
    public AtomicLong getLastCacheChanged() {
        return lastCacheChanged;
    }

    public String toString() {
        return getUrl().toString();
    }



    private class SaveProperties implements Runnable {
        private long version;

        private SaveProperties(long version) {
            this.version = version;
        }

        public void run() {
            doSaveProperties(version);
        }
    }

}