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
package com.alibaba.dubbo.common.extension;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.support.ActivateComparator;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.Holder;
import com.alibaba.dubbo.common.utils.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * Load dubbo extensions
 * <ul>
 * <li>auto inject dependency extension </li>
 * <li>auto wrap extension in wrapper </li>
 * <li>default extension is an adaptive instance</li>
 * </ul>
 *
 * @see <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jar/jar.html#Service%20Provider">Service Provider in Java 5</a>
 * @see com.alibaba.dubbo.common.extension.SPI
 * @see com.alibaba.dubbo.common.extension.Adaptive
 * @see com.alibaba.dubbo.common.extension.Activate
 *
 *
 *

参考：https://blog.csdn.net/chs007chs/article/details/56830748

SPI

    SPI只是一种协议，它只是规定在META-INF目录下提供接口的实现描述文件，由框架本身定义接口、规范，第三方只需要将自己实现
    在META-INF下描述清楚，那么框架就会自动加载你的实现。比如Dubbo的规则是在META-INF/dubbo、META-INF/dubbo/internal或者
    META-INF/services下面以需要实现的接口全面去创建一个文件，并且在文件中以properties规则一样配置实现类的全面以及分配实
    现一个名称。


ExtensionLoader

    Dubbo对于SPI的实现全部集中在类ExtensionLoader中，ExtensionLoader是一个单例工厂类，它对外暴露getExtensionLoader静态方
    法返回一个ExtensionLoader实体，这个方法的入参是一个Class类型，这个方法的意思是返回某个接口的ExtensionLoader，对于一
    个接口，只会有一个ExtensionLoader实体。

    ExtensionLoader实体对外暴露一些接口来获取扩展实现，这些接口分为几类，分别是activate extension, adaptive extension,
    default extension, get extension by name , supported extension。

（1）URL为总线的模式

    即运行过程中所有的状态信息都可以通过URL来获取。

（2）activate extension

    activate extension都需要传入url参数，这里涉及到Activate 注解，这个注解主要用于标注在插件接口实现类上，用来配置该扩展
    实现类激活条件。

    在Dubbo框架里 面的Filter的各种实现类都通过Activate标注，用来描述该Filter什么时候生效。

    MonitorFillter通过Activate标注来告诉Dubbo框架这个Filter是在服务提供端和消费端会生效的。
    TimeoutFilter则只在服务提供端生效。
    ValidationFilter除了在消费端和服务提供端激活，它还配置value，这是另一个激活条件，这个value表示传入的URL参数中必须有
    指定的值才可激活这个扩展。

    另个activate注解还有一个参数order，这是表示一种排序规则，因为一个接口的实现有多种，返回的结果是一个列表，如果不指定
    排序规则，那么可能列表的排序不可控，其中order的值越大，那么该扩展实现排序就越靠前，对于排序还可以使用before和after来
    配置。对于用户自定义的扩展默认是追加到列表后面的。

    getActivateExtension(URL url, String[] values, String group)

（3）adaptive extension
    Dubbo框架提供的各种接口均有很多种类的实现，为了能够适配一个接口的各种实现，便有了adaptive extension。
    createAdaptiveExtensionClassCode。

    对某个接口实现对应的适配器。
    对于这种途径，Dubbo也提供了一个注解Adaptive，用来标注在接口的某个实现上，表示这个实现并不是提供具体业务支持，而是作
    为该接口的适配器。
    Dubbo框架中的AdaptiveExtensionFatory就是使用Adaptive进行了标注，它用来适ExtensionFactory接口SPIExtensionFactory和
    SpringExtensionFactory两种实现 ，它会根据支行的状态来确定具体调用ExtensionFactory的哪个实现。
    Dubbo框架动态生成适配器。
    ExtensionLoader通过分配接口配置的adaptive规则动态生成adaptive类并且加载到ClassLoader中，来实现动态适配。adaptive注解
    有一个value属性，通过设置这个属性便可以设置该接口的Adaptive的规则，Dubbo动态生成Adaptive的扩展接口的方法入参必须包含
    URL或者参数存在能返回URL对象的方法，这样才能根据支行状态动态选择具体实现。


（4）get extension by name
    就是通过接口实现的别名来获取某个具体的服务。

（5）default extension
    被实现的接口必须标注SPI注解，用来告诉Dubbo这个接口是通过SPI来进行扩展实现的，在接口上标注SPI注解的时候可以配置一个
    value属性用来描述这个接口的默认实现别名。

应用场景
    在dubbo中随处可见ExtensionLoader的使用，几乎任何预留扩展的服务都通过ExtensionLoader加载。
    SPI，扩展点接口标识，只有添加了这个注解才能通过ExtensionLoader加载其服务实现。
    Adaptive，标识此类为Adaptive类（dubbo可自动生成）。
    Activate，用于标识此实现是否激活，可以配置一些激活条件。
    以ReferencConfig为例，其中的Protocol等便是通过ExtensionLoader来加载的。

（1）首先通过getExtensionLoader方法获取对应的Protocol的ExtensionLoader实例。
（2）之后便是通过getAdaptiveExtension()获取对应的Adaptive实现。
（3）Adaptive实例不存在，则通过createAdaptiveExtension()创建，在这之前要加载Adaptive Class。
（4）如果找不到被Adaptive注解的适配器，则通过dubbo动态创建（字节码）适配器。
（5）由injectExtension完成依赖服务注入（借助ExtensionFactory找到依赖的服务，通过set方法注入）。


Dubbo的扩展点主要有adaptive和wrapper两种：

（1）adaptive，因为dubbo底层会大量使用反射，出于性能考虑默认使用javassist字节码编译生成一个adaptive，由它动态委派处理。用户可以
    自己实现一个adaptive，只需要对某个类打上@adaptive即可。对于默认编译生成Adaptive的方案，需要使用@Adaptive声明接口上的哪些方
    法是adaptive方法。扩展点名称的key默认是接口类型上@SPI#value，方法上的@Adaptive#value有更高优先级。

（2）包装类必须有一个参数为spi接口类型的构造函数，否则不能正常工作。判断warpper的标准是class有没有一个参数为接口类型的构造参数。
    Wrapper可以有多个，会被按顺序依次覆盖，假设spi定义如下：

    A=a.b.c
    B=a.b.wrapper1
    C=a.b.wrapper2

    wrapper的最终结构则为B-C-A

 */
public class ExtensionLoader<T> {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);

    private static final String SERVICES_DIRECTORY = "META-INF/services/";
    private static final String DUBBO_DIRECTORY = "META-INF/dubbo/";
    private static final String DUBBO_INTERNAL_DIRECTORY = DUBBO_DIRECTORY + "internal/";

    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");
    /** 用于保存不同SPI接口对应的ExtensionLoader，key：对应SPI接口的类型，value：SPI接口对应的ExtensionLoader */
    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<Class<?>, ExtensionLoader<?>>();
    /** 缓存SPI接口类型对应的扩展点实现类 */
    private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<Class<?>, Object>();

    // ==============================
    /** 表示一个SPI接口的类型 */
    private final Class<?> type;
    /** 扩展点工厂，用于创建扩展点的实现类，即SPI接口的实现类(该扩展点工厂本身也是一个SPI接口，) */
    private final ExtensionFactory objectFactory;
    /** 缓存（key：扩展点实现类型，value：扩展点名称），同{@link #cachedClasses}*/
    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<Class<?>, String>();
    /** 保存{@link #type}对应的SPI接口的扩展实现，key:扩展实现名称，value:对应的扩展实现的类型 */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<Map<String, Class<?>>>();
    /** 如果扩展点实现有被@Activate修饰，则会被缓存到这里，key：表示扩展点名称，value：修饰的该扩展点的@Activate注解对象 */
    private final Map<String, Activate> cachedActivates = new ConcurrentHashMap<String, Activate>();
    /** 缓存扩展点名称，及对应的扩展点实例*/
    private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<String, Holder<Object>>();
    /** 缓存这个{@link #type} 对应的扩展点实例 */
    private final Holder<Object> cachedAdaptiveInstance = new Holder<Object>();
    /** 表示有被@Adaptive注解修饰的扩展点，目前整个系统只有2个，AdaptiveCompiler、AdaptiveExtensionFactory */
    private volatile Class<?> cachedAdaptiveClass = null;
    /** 表示这个SPI接口的默认实现，对应@SPI注解的value值，参见{@link ExtensionLoader#loadExtensionClasses()}*/
    private String cachedDefaultName;
    /** 表示创建扩展点实例过程中出现的异常 */
    private volatile Throwable createAdaptiveInstanceError;
    /**
     * 用于缓存包装器的扩展点，例如：ProtocolListenerWrapper和ProtocolFilterWrapper
     * 判断是否为包装器的依据是，是否带有例如如下的构造器：public ProtocolFilterWrapper(Protocol protocol){};
     *
     */
    private Set<Class<?>> cachedWrapperClasses;
    private Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<String, IllegalStateException>();



    private ExtensionLoader(Class<?> type) {
        this.type = type;

        objectFactory = (type == ExtensionFactory.class ? null :
                ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension());
    }


    /**
     * 根据SPI接口返回一个ExtensionLoader实例，然后在调用{@link #getAdaptiveExtension()}方法返回该SPI接口的实现
     * 在dubbo中，每个SPI接口都对应一个不同的ExtensionLoader实例，实例化后的ExtensionLoader实例会保存在{@link #EXTENSION_LOADERS}中
     *
     * @param type  表示一个SPI的扩展接口，如果入参 type 不是SPI接口，则会抛出异常
     * @param <T>   表示该SPI接口的一个实现实例
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null)
            throw new IllegalArgumentException("Extension type == null");
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type(" + type + ") is not interface!");
        }
        // 扩展点接口必须有@SPI注解
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension type(" + type +
                    ") is not extension, because WITHOUT @" + SPI.class.getSimpleName() + " Annotation!");
        }

        // 从缓存获取扩展点对应的 ExtensionLoader，如果没有获取到加载一个扩展点的ExtensionLoader实例
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }


    /**
     * 表示这个SPI接口的默认实现，对应@SPI注解的value值，如果没有配置的话，返回null
     */
    public String getDefaultExtensionName() {
        getExtensionClasses();
        return cachedDefaultName;
    }
    /**
     * 返回该ExtensionLoader已经加载的所有扩展点名称
     *
     * Return the list of extensions which are already loaded.
     * <p>
     * Usually {@link #getSupportedExtensions()} should be called in order to get all extensions.
     *
     * @see #getSupportedExtensions()
     */
    public Set<String> getLoadedExtensions() {
        return Collections.unmodifiableSet(new TreeSet<String>(cachedInstances.keySet()));
    }
    public Set<String> getSupportedExtensions() {
        Map<String, Class<?>> clazzes = getExtensionClasses();
        return Collections.unmodifiableSet(new TreeSet<String>(clazzes.keySet()));
    }
    /**
     * 判断这个name是有对应的扩展点实现
     * @param name  这里的name使用的是简称，比如：协议扩展dubbo、rmi、http等
     * @return
     */
    public boolean hasExtension(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Extension name == null");
        try {
            return getExtensionClass(name) != null;
        } catch (Throwable t) {
            return false;
        }
    }
    /**
     * 获取name对应的扩展点实现
     * @param name  这里的name使用的是简称，比如：协议扩展dubbo、rmi、http等
     * @return
     */
    private Class<?> getExtensionClass(String name) {
        if (type == null)
            throw new IllegalArgumentException("Extension type == null");
        if (name == null)
            throw new IllegalArgumentException("Extension name == null");
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null)
            throw new IllegalStateException("No such extension \"" + name + "\" for " + type.getName() + "!");
        return clazz;
    }
    /**
     * 判断这个class是否有@SPI注解
     * @param type
     * @param <T>
     * @return
     */
    private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }



    // -------------------------------
    // 根据扩展点实现实例获取扩展点名称
    // -------------------------------

    /**
     * 根据扩展点实例，获取该扩展点名称
     * @param extensionInstance
     * @return
     */
    public String getExtensionName(T extensionInstance) {
        return getExtensionName(extensionInstance.getClass());
    }
    /**
     * 根据扩展点实例类型，获取该扩展点名称
     * @param extensionClass
     * @return
     */
    public String getExtensionName(Class<?> extensionClass) {
        return cachedNames.get(extensionClass);
    }




    // -------------------------------
    // 根据扩展点名称获取扩展点实现实例
    // -------------------------------

    /**
     * Get extension's instance. Return <code>null</code> if extension is not found or is not initialized. Pls. note
     * that this method will not trigger extension load.
     * <p>
     * In order to trigger extension load, call {@link #getExtension(String)} instead.
     *
     * @see #getExtension(String)
     */
    @SuppressWarnings("unchecked")
    public T getLoadedExtension(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Extension name == null");
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<Object>());
            holder = cachedInstances.get(name);
        }
        return (T) holder.get();
    }
    /**
     * 根据给与的扩展点名称获取扩展点实例：
     *
     * 根据这个name返回对应的SPI实现类，注意，这里返回的SPI实现类，使用了装饰器模式，返回的实现类可能不是直接指定的实现
     * 类，比如：使用dubbo协议时，按说这里的返回的实现类应该是 DubboProtocol ，但是实际上可能并不是，这里返回的
     * 可能是 ProtocolListenerWrapper ，ProtocolListenerWrapper内部存在一个Protocol引用，指向 ProtocolFilterWrapper
     * 然后，ProtocolFilterWrapper内部的Protocol引用指向 RegistryProtocol ，最后RegistryProtocol的内部Protocol才
     * 指向 DubboProtocol
     *
     * @param name  如果name是“true”的话，则返回默认的扩展点实例
     * @return
     */
    @SuppressWarnings("unchecked")
    public T getExtension(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Extension name == null");

        if ("true".equals(name)) {
            return getDefaultExtension();
        }

        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<Object>());
            holder = cachedInstances.get(name);
        }
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }
    /**
     * 该扩展加载器，默认的扩展点实例
     */
    public T getDefaultExtension() {
        getExtensionClasses();
        if (null == cachedDefaultName || cachedDefaultName.length() == 0
                || "true".equals(cachedDefaultName)) {
            return null;
        }
        return getExtension(cachedDefaultName);
    }
    @SuppressWarnings("unchecked")
    private T createExtension(String name) {
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw findException(name);
        }
        try {
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                // 比如扩展点名为dubbo，则这里会创建一个DubboProtocol的实例
                EXTENSION_INSTANCES.putIfAbsent(clazz, (T) clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }
            // 扩展点的装饰器模式就是在这里实现，注入被装饰的扩展点实例
            injectExtension(instance);
            Set<Class<?>> wrapperClasses = cachedWrapperClasses;
            if (wrapperClasses != null && wrapperClasses.size() > 0) {
                // 如果有包装器的话，这里返回实例前都会用包装器一层一层的包装，最终调用的扩展点的真正实现
                for (Class<?> wrapperClass : wrapperClasses) {
                    instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
                }
            }
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Extension instance(name: " + name + ", class: " +
                    type + ")  could not be instantiated: " + t.getMessage(), t);
        }
    }
    private IllegalStateException findException(String name) {
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (entry.getKey().toLowerCase().contains(name.toLowerCase())) {
                return entry.getValue();
            }
        }
        StringBuilder buf = new StringBuilder("No such extension " + type.getName() + " by name " + name);


        int i = 1;
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (i == 1) {
                buf.append(", possible causes: ");
            }

            buf.append("\r\n(");
            buf.append(i++);
            buf.append(") ");
            buf.append(entry.getKey());
            buf.append(":\r\n");
            buf.append(StringUtils.toString(entry.getValue()));
        }
        return new IllegalStateException(buf.toString());
    }




    // ----------------------------------------
    // 自定义扩展点（用于扩展自定义的扩展点实现）
    // ----------------------------------------

    /**
     * 添加一个新扩展点实现，该方法用于扩展
     * Register new extension via API
     *
     * @param name  表示扩展点名称
     * @param clazz 表示该扩展点名称对应的实现类的类型，如果扩展点没有@Adaptive注解修饰的话，加载时会异常
     * @throws IllegalStateException when extension with the same name has already been registered.
     */
    public void addExtension(String name, Class<?> clazz) {
        getExtensionClasses(); // load classes

        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Input type " +
                    clazz + "not implement Extension " + type);
        }
        if (clazz.isInterface()) {
            throw new IllegalStateException("Input type " +
                    clazz + "can not be interface!");
        }

        if (!clazz.isAnnotationPresent(Adaptive.class)) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("Extension name is blank (Extension " + type + ")!");
            }
            if (cachedClasses.get().containsKey(name)) {
                throw new IllegalStateException("Extension name " +
                        name + " already existed(Extension " + type + ")!");
            }

            cachedNames.put(clazz, name);
            cachedClasses.get().put(name, clazz);
        } else {
            if (cachedAdaptiveClass != null) {
                throw new IllegalStateException("Adaptive Extension already existed(Extension " + type + ")!");
            }

            cachedAdaptiveClass = clazz;
        }
    }
    /**
     * Replace the existing extension via API
     *
     * @param name  extension name
     * @param clazz extension class
     * @throws IllegalStateException when extension to be placed doesn't exist
     * @deprecated not recommended any longer, and use only when test
     */
    @Deprecated
    public void replaceExtension(String name, Class<?> clazz) {
        getExtensionClasses(); // load classes

        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Input type " +
                    clazz + "not implement Extension " + type);
        }
        if (clazz.isInterface()) {
            throw new IllegalStateException("Input type " +
                    clazz + "can not be interface!");
        }

        if (!clazz.isAnnotationPresent(Adaptive.class)) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("Extension name is blank (Extension " + type + ")!");
            }
            if (!cachedClasses.get().containsKey(name)) {
                throw new IllegalStateException("Extension name " +
                        name + " not existed(Extension " + type + ")!");
            }

            cachedNames.put(clazz, name);
            cachedClasses.get().put(name, clazz);
            cachedInstances.remove(name);
        } else {
            if (cachedAdaptiveClass == null) {
                throw new IllegalStateException("Adaptive Extension not existed(Extension " + type + ")!");
            }

            cachedAdaptiveClass = clazz;
            cachedAdaptiveInstance.set(null);
        }
    }





    // -----------------
    // 读取扩展点配置文件
    // -----------------

    /**
     * synchronized in getExtensionClasses
     *
     * 返回这个{@link #type}对应的SPI接口的扩展实现
     *
     * 分别从"META-INF/services/"、"META-INF/dubbo/" 和 "META-INF/dubbo/internal/" 这三个目录下去加载扩展点
     *
     * @see #SERVICES_DIRECTORY          "META-INF/services/"
     * @see #DUBBO_DIRECTORY             "META-INF/dubbo/"
     * @see #DUBBO_INTERNAL_DIRECTORY    "META-INF/dubbo/internal/"
     *
     * @return  key:扩展实现名称(这里使用的是简称，比如：协议扩展dubbo、rmi、http等)，value:对应的扩展实现的类型
     */
    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = loadExtensionClasses();
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }
    /**
     * synchronized in getExtensionClasses
     *
     * 返回这个{@link #type}对应的SPI接口的扩展实现
     *
     * 分别从"META-INF/services/"、"META-INF/dubbo/" 和 "META-INF/dubbo/internal/" 这三个目录下去加载扩展点
     *
     * @see #SERVICES_DIRECTORY          "META-INF/services/"
     * @see #DUBBO_DIRECTORY             "META-INF/dubbo/"
     * @see #DUBBO_INTERNAL_DIRECTORY    "META-INF/dubbo/internal/"
     *
     * @return  key:扩展实现名称，value:对应的扩展实现的类型
     */
    private Map<String, Class<?>> loadExtensionClasses() {
        // 获取SPI接口的配置信息
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        if (defaultAnnotation != null) {
            String value = defaultAnnotation.value();
            if (value != null && (value = value.trim()).length() > 0) {
                String[] names = NAME_SEPARATOR.split(value);
                if (names.length > 1) {
                    throw new IllegalStateException("more than 1 default extension name on extension " + type.getName()
                            + ": " + Arrays.toString(names));
                }
                if (names.length == 1) cachedDefaultName = names[0];
            }
        }

        Map<String, Class<?>> extensionClasses = new HashMap<String, Class<?>>();
        loadFile(extensionClasses, DUBBO_INTERNAL_DIRECTORY);
        loadFile(extensionClasses, DUBBO_DIRECTORY);
        loadFile(extensionClasses, SERVICES_DIRECTORY);
        return extensionClasses;
    }
    /**
     * 扫描SPI接口实现类时，如果类上有{@link Adaptive}注解则将类的class对象缓存到{@link #cachedAdaptiveClass}。
     * 然后在调用{@link #getAdaptiveExtension()}方法时，如果cachedAdaptiveClass不为空，则返回缓存否则调
     * 用{@link #createAdaptiveExtensionClassCode()} 方法生成扩展类
     *
     * @param extensionClasses
     * @param dir
     */
    private void loadFile(Map<String, Class<?>> extensionClasses, String dir) {
        // 表示这个SPI接口对应的扩展文件
        String fileName = dir + type.getName();
        try {
            Enumeration<java.net.URL> urls;
            ClassLoader classLoader = findClassLoader();
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    java.net.URL url = urls.nextElement();
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                        try {
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                final int ci = line.indexOf('#');
                                if (ci >= 0) line = line.substring(0, ci);
                                line = line.trim();
                                if (line.length() > 0) {
                                    try {
                                        String name = null;
                                        int i = line.indexOf('=');
                                        if (i > 0) {
                                            // 扩展点名称
                                            name = line.substring(0, i).trim();
                                            // 扩展点实现类
                                            line = line.substring(i + 1).trim();
                                        }
                                        if (line.length() > 0) {
                                            Class<?> clazz = Class.forName(line, true, classLoader);
                                            if (!type.isAssignableFrom(clazz)) {
                                                throw new IllegalStateException("Error when load extension class(interface: " +
                                                        type + ", class line: " + clazz.getName() + "), class "
                                                        + clazz.getName() + "is not subtype of interface.");
                                            }

                                            // 判断这个clazz是否被Adaptive注解修饰：目前整个系统只有2个，AdaptiveCompiler、AdaptiveExtensionFactory
                                            if (clazz.isAnnotationPresent(Adaptive.class)) {
                                                if (cachedAdaptiveClass == null) {
                                                    cachedAdaptiveClass = clazz;
                                                } else if (!cachedAdaptiveClass.equals(clazz)) {
                                                    throw new IllegalStateException("More than 1 adaptive class found: "
                                                            + cachedAdaptiveClass.getClass().getName()
                                                            + ", " + clazz.getClass().getName());
                                                }
                                            } else {
                                                try {
                                                    // 获取一个例如：public DubboProtocol(Protocol protocol){}这样的构造器
                                                    // 例如：ProtocolListenerWrapper和ProtocolFilterWrapper才有这样的构造器
                                                    clazz.getConstructor(type);
                                                    Set<Class<?>> wrappers = cachedWrapperClasses;
                                                    if (wrappers == null) {
                                                        cachedWrapperClasses = new ConcurrentHashSet<Class<?>>();
                                                        wrappers = cachedWrapperClasses;
                                                    }
                                                    wrappers.add(clazz);
                                                } catch (NoSuchMethodException e) {
                                                    // 获取默认的构造器，一般的SPI扩展点都会有这个构造器
                                                    clazz.getConstructor();
                                                    // 如果这个扩展点没有名称的情况，比如：HttpProtocol这个扩展点，则直接使用http这个前缀作为扩展点名称
                                                    // 另外，如果HttpProtocol这个扩展点有被@Extension这个注解修饰的话，会使用注解值作为扩展点名称
                                                    if (name == null || name.length() == 0) {
                                                        name = findAnnotationName(clazz);
                                                        if (name == null || name.length() == 0) {
                                                            if (clazz.getSimpleName().length() > type.getSimpleName().length()
                                                                    && clazz.getSimpleName().endsWith(type.getSimpleName())) {
                                                                name = clazz.getSimpleName().substring(0, clazz.getSimpleName().length() - type.getSimpleName().length()).toLowerCase();
                                                            } else {
                                                                throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + url);
                                                            }
                                                        }
                                                    }

                                                    String[] names = NAME_SEPARATOR.split(name);
                                                    if (names != null && names.length > 0) {
                                                        Activate activate = clazz.getAnnotation(Activate.class);
                                                        if (activate != null) {
                                                            cachedActivates.put(names[0], activate);
                                                        }
                                                        for (String n : names) {
                                                            if (!cachedNames.containsKey(clazz)) {
                                                                cachedNames.put(clazz, n);
                                                            }
                                                            Class<?> c = extensionClasses.get(n);
                                                            if (c == null) {
                                                                extensionClasses.put(n, clazz);
                                                            } else if (c != clazz) {
                                                                throw new IllegalStateException("Duplicate extension " + type.getName() + " name " + n + " on " + c.getName() + " and " + clazz.getName());
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Throwable t) {
                                        IllegalStateException e = new IllegalStateException("Failed to load extension class(interface: " + type + ", class line: " + line + ") in " + url + ", cause: " + t.getMessage(), t);
                                        exceptions.put(line, e);
                                    }
                                }
                            } // end of while read lines
                        } finally {
                            reader.close();
                        }
                    } catch (Throwable t) {
                        logger.error("Exception when load extension class(interface: " +
                                type + ", class file: " + url + ") in " + url, t);
                    }
                } // end of while urls
            }
        } catch (Throwable t) {
            logger.error("Exception when load extension class(interface: " +
                    type + ", description file: " + fileName + ").", t);
        }
    }
    private static ClassLoader findClassLoader() {
        return ExtensionLoader.class.getClassLoader();
    }

    /**
     * 如果clazz这个扩展点有@Extension修饰，则直接获取这个注解的值，如果没有，比如：HttpProtocol，则返回http这个前缀
     * @param clazz
     * @return
     */
    @SuppressWarnings("deprecation")
    private String findAnnotationName(Class<?> clazz) {
        com.alibaba.dubbo.common.Extension extension = clazz.getAnnotation(com.alibaba.dubbo.common.Extension.class);
        if (extension == null) {
            String name = clazz.getSimpleName();
            if (name.endsWith(type.getSimpleName())) {
                name = name.substring(0, name.length() - type.getSimpleName().length());
            }
            return name.toLowerCase();
        }
        return extension.value();
    }




    // --------------
    // 创建扩展点实例
    // --------------

    /**
     * 返回类型 T（SPI接口） 对应的扩展点实例
     * @return
     */
    @SuppressWarnings("unchecked")
    public T getAdaptiveExtension() {
        Object instance = cachedAdaptiveInstance.get();
        if (instance == null) {
            if (createAdaptiveInstanceError == null) {
                synchronized (cachedAdaptiveInstance) {
                    instance = cachedAdaptiveInstance.get();
                    if (instance == null) {
                        try {
                            // 创建一个扩展点实例
                            instance = createAdaptiveExtension();
                            cachedAdaptiveInstance.set(instance);
                        } catch (Throwable t) {
                            createAdaptiveInstanceError = t;
                            throw new IllegalStateException("fail to create adaptive instance: " + t.toString(), t);
                        }
                    }
                }
            } else {
                throw new IllegalStateException("fail to create adaptive instance: " + createAdaptiveInstanceError.toString(), createAdaptiveInstanceError);
            }
        }

        return (T) instance;
    }
    /**
     * 创建扩展点实例
     *
     * @return  返回 T 对应的扩展点实例
     */
    @SuppressWarnings("unchecked")
    private T createAdaptiveExtension() {
        try {
            return injectExtension((T) getAdaptiveExtensionClass().newInstance());
        } catch (Exception e) {
            throw new IllegalStateException("Can not create adaptive extension " + type + ", cause: " + e.getMessage(), e);
        }
    }
    /**
     * 获取这个扩展点的实现类型
     * @return
     */
    private Class<?> getAdaptiveExtensionClass() {
        getExtensionClasses();
        if (cachedAdaptiveClass != null) {
            return cachedAdaptiveClass;
        }
        return cachedAdaptiveClass = createAdaptiveExtensionClass();
    }
    /**
     * 生成一个动态的扩展点实现类，Dubbo将扩展点的扩展名保存到URL对象中，在调用时通过“扩展点自适应机制”找到对应的扩展
     * 实现，这里的“扩展点自适应机制”就是在该方法中实现的，通过{@link #createAdaptiveExtensionClassCode()}
     * 方法生成代码，在通过{@link com.alibaba.dubbo.common.compiler.support.AdaptiveCompiler}生成动态类的编译类
     *
     * 1. 获取某个SPI接口的adaptive实现类的规则是：
     *  （1）实现类的类上面有Adaptive注解的，那么这个类就是adaptive类
     *  （2）实现类的类上面没有Adaptive注解，但是在方法上有Adaptive注解，则会动态生成adaptive类
     *
     *  2 .生成的动态类的编译类是：com.alibaba.dubbo.common.compiler.support.AdaptiveCompiler类
     *  3. 动态类的本质是可以做到一个SPI中的不同的Adaptive方法可以去调不同的SPI实现类去处理。使得程序的灵活性大大提高。这才是整套SPI设计的一个精华之所在
     *
     * @return
     */
    private Class<?> createAdaptiveExtensionClass() {
/* 示例：

public class Protocol$Adaptive implements com.alibaba.dubbo.rpc.Protocol {
    public void destroy() {
        throw new UnsupportedOperationException(
            "method public abstract void com.alibaba.dubbo.rpc.Protocol.destroy() of interface com.alibaba.dubbo.rpc"
                + ".Protocol is not adaptive method!");
    }

    public int getDefaultPort() {
        throw new UnsupportedOperationException(
            "method public abstract int com.alibaba.dubbo.rpc.Protocol.getDefaultPort() of interface com.alibaba"
                + ".dubbo.rpc.Protocol is not adaptive method!");
    }

    public com.alibaba.dubbo.rpc.Exporter export(com.alibaba.dubbo.rpc.Invoker arg0) throws com.alibaba.dubbo.rpc.RpcException {

        if (arg0 == null) { throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument == null"); }
        if (arg0.getUrl() == null) {
            throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument getUrl() == null");
        }
        com.alibaba.dubbo.common.URL url = arg0.getUrl();
        String extName = (url.getProtocol() == null ? "dubbo" : url.getProtocol());
        if (extName == null) {
            throw new IllegalStateException(
                "Fail to get extension(com.alibaba.dubbo.rpc.Protocol) name from url(" + url.toString()
                    + ") use keys([protocol])");
        }
        com.alibaba.dubbo.rpc.Protocol extension = (com.alibaba.dubbo.rpc.Protocol)ExtensionLoader.getExtensionLoader(
            com.alibaba.dubbo.rpc.Protocol.class).getExtension(extName);
        return extension.export(arg0);
    }

    public com.alibaba.dubbo.rpc.Invoker refer(java.lang.Class arg0, com.alibaba.dubbo.common.URL arg1) throws com.alibaba.dubbo.rpc.RpcException {
        if (arg1 == null) { throw new IllegalArgumentException("url == null"); }
        com.alibaba.dubbo.common.URL url = arg1;
        String extName = (url.getProtocol() == null ? "dubbo" : url.getProtocol());
        if (extName == null) {
            throw new IllegalStateException(
                "Fail to get extension(com.alibaba.dubbo.rpc.Protocol) name from url(" + url.toString()
                    + ") use keys([protocol])");
        }
        com.alibaba.dubbo.rpc.Protocol extension = (com.alibaba.dubbo.rpc.Protocol)ExtensionLoader.getExtensionLoader(
            com.alibaba.dubbo.rpc.Protocol.class).getExtension(extName);
        return extension.refer(arg0, arg1);
    }
}
*/
        // 上面是这里生成的动态类代码的示例
        String code = createAdaptiveExtensionClassCode();
        ClassLoader classLoader = findClassLoader();
        com.alibaba.dubbo.common.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
        return compiler.compile(code, classLoader);
    }
    private String createAdaptiveExtensionClassCode() {
        StringBuilder codeBuidler = new StringBuilder();
        Method[] methods = type.getMethods();
        boolean hasAdaptiveAnnotation = false;
        for (Method m : methods) {
            if (m.isAnnotationPresent(Adaptive.class)) {
                hasAdaptiveAnnotation = true;
                break;
            }
        }
        // no need to generate adaptive class since there's no adaptive method found.如果接口一个@Adaptive修饰的方法都没有就报错
        if (!hasAdaptiveAnnotation)
            throw new IllegalStateException("No adaptive method on extension " + type.getName() + ", refuse to create the adaptive class!");

        codeBuidler.append("package " + type.getPackage().getName() + ";");
        codeBuidler.append("\nimport " + ExtensionLoader.class.getName() + ";");
        codeBuidler.append("\npublic class " + type.getSimpleName() + "$Adaptive" + " implements " + type.getCanonicalName() + " {");

        for (Method method : methods) {
            Class<?> rt = method.getReturnType();
            Class<?>[] pts = method.getParameterTypes();
            Class<?>[] ets = method.getExceptionTypes();

            Adaptive adaptiveAnnotation = method.getAnnotation(Adaptive.class);
            StringBuilder code = new StringBuilder(512);
            // 没有@Adaptive修饰的方法都将方法实现设置为：throw new UnsupportedOperationException("method of interface is not adaptive method!);
            if (adaptiveAnnotation == null) {
                code.append("throw new UnsupportedOperationException(\"method ")
                        .append(method.toString()).append(" of interface ")
                        .append(type.getName()).append(" is not adaptive method!\");");
            } else {
                // 遍历查找URL.class类型的入参
                int urlTypeIndex = -1;
                for (int i = 0; i < pts.length; ++i) {
                    if (pts[i].equals(URL.class)) {
                        urlTypeIndex = i;
                        break;
                    }
                }

                // found parameter in URL type
                if (urlTypeIndex != -1) {
                    // Null Point check
                    // 存在URL.class类型的入参时，如果入参是null则抛IllegalArgumentException异常
                    String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"url == null\");",
                            urlTypeIndex);
                    code.append(s);

                    // 存在URL.class类型的入参时，如果入参不是null，则设置一个url的局部变量并将入参赋值给它
                    s = String.format("\n%s url = arg%d;", URL.class.getName(), urlTypeIndex);
                    code.append(s);
                }
                // did not find parameter in URL type
                else {
                    String attribMethod = null;

                    // find URL getter method
                    // 查找获取URL的getter方法
                    LBL_PTS:
                    for (int i = 0; i < pts.length; ++i) {
                        Method[] ms = pts[i].getMethods();
                        for (Method m : ms) {
                            String name = m.getName();
                            if ((name.startsWith("get") || name.length() > 3)
                                    && Modifier.isPublic(m.getModifiers())
                                    && !Modifier.isStatic(m.getModifiers())
                                    && m.getParameterTypes().length == 0
                                    && m.getReturnType() == URL.class) {
                                urlTypeIndex = i;
                                attribMethod = name;
                                break LBL_PTS;
                            }
                        }
                    }
                    if (attribMethod == null) {
                        throw new IllegalStateException("fail to create adaptive class for interface " + type.getName()
                                + ": not found url parameter or url attribute in parameters of method " + method.getName());
                    }

                    // Null point check
                    String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"%s argument == null\");",
                            urlTypeIndex, pts[urlTypeIndex].getName());
                    code.append(s);
                    s = String.format("\nif (arg%d.%s() == null) throw new IllegalArgumentException(\"%s argument %s() == null\");",
                            urlTypeIndex, attribMethod, pts[urlTypeIndex].getName(), attribMethod);
                    code.append(s);

                    s = String.format("%s url = arg%d.%s();", URL.class.getName(), urlTypeIndex, attribMethod);
                    code.append(s);
                }

                String[] value = adaptiveAnnotation.value();
                // value is not set, use the value generated from class name as the key
                if (value.length == 0) {
                    char[] charArray = type.getSimpleName().toCharArray();
                    StringBuilder sb = new StringBuilder(128);
                    for (int i = 0; i < charArray.length; i++) {
                        if (Character.isUpperCase(charArray[i])) {
                            if (i != 0) {
                                sb.append(".");
                            }
                            sb.append(Character.toLowerCase(charArray[i]));
                        } else {
                            sb.append(charArray[i]);
                        }
                    }
                    value = new String[]{sb.toString()};
                }

                boolean hasInvocation = false;
                for (int i = 0; i < pts.length; ++i) {
                    if (pts[i].getName().equals("com.alibaba.dubbo.rpc.Invocation")) {
                        // Null Point check
                        String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"invocation == null\");", i);
                        code.append(s);
                        s = String.format("\nString methodName = arg%d.getMethodName();", i);
                        code.append(s);
                        hasInvocation = true;
                        break;
                    }
                }

                String defaultExtName = cachedDefaultName;
                String getNameCode = null;
                for (int i = value.length - 1; i >= 0; --i) {
                    if (i == value.length - 1) {
                        if (null != defaultExtName) {
                            if (!"protocol".equals(value[i]))
                                if (hasInvocation)
                                    getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                                else
                                    getNameCode = String.format("url.getParameter(\"%s\", \"%s\")", value[i], defaultExtName);
                            else
                                getNameCode = String.format("( url.getProtocol() == null ? \"%s\" : url.getProtocol() )", defaultExtName);
                        } else {
                            if (!"protocol".equals(value[i]))
                                if (hasInvocation)
                                    getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                                else
                                    getNameCode = String.format("url.getParameter(\"%s\")", value[i]);
                            else
                                getNameCode = "url.getProtocol()";
                        }
                    } else {
                        if (!"protocol".equals(value[i]))
                            if (hasInvocation)
                                getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                            else
                                getNameCode = String.format("url.getParameter(\"%s\", %s)", value[i], getNameCode);
                        else
                            getNameCode = String.format("url.getProtocol() == null ? (%s) : url.getProtocol()", getNameCode);
                    }
                }
                code.append("\nString extName = ").append(getNameCode).append(";");
                // check extName == null?
                String s = String.format("\nif(extName == null) " +
                                "throw new IllegalStateException(\"Fail to get extension(%s) name from url(\" + url.toString() + \") use keys(%s)\");",
                        type.getName(), Arrays.toString(value));
                code.append(s);

                s = String.format("\n%s extension = (%<s)%s.getExtensionLoader(%s.class).getExtension(extName);",
                        type.getName(), ExtensionLoader.class.getSimpleName(), type.getName());
                code.append(s);

                // return statement
                if (!rt.equals(void.class)) {
                    code.append("\nreturn ");
                }

                s = String.format("extension.%s(", method.getName());
                code.append(s);
                for (int i = 0; i < pts.length; i++) {
                    if (i != 0)
                        code.append(", ");
                    code.append("arg").append(i);
                }
                code.append(");");
            }

            codeBuidler.append("\npublic " + rt.getCanonicalName() + " " + method.getName() + "(");
            for (int i = 0; i < pts.length; i++) {
                if (i > 0) {
                    codeBuidler.append(", ");
                }
                codeBuidler.append(pts[i].getCanonicalName());
                codeBuidler.append(" ");
                codeBuidler.append("arg" + i);
            }
            codeBuidler.append(")");
            if (ets.length > 0) {
                codeBuidler.append(" throws ");
                for (int i = 0; i < ets.length; i++) {
                    if (i > 0) {
                        codeBuidler.append(", ");
                    }
                    codeBuidler.append(ets[i].getCanonicalName());
                }
            }
            codeBuidler.append(" {");
            codeBuidler.append(code.toString());
            codeBuidler.append("\n}");
        }
        codeBuidler.append("\n}");
        if (logger.isDebugEnabled()) {
            logger.debug(codeBuidler.toString());
        }
        return codeBuidler.toString();
    }
    /**
     * 为这个扩展点实例注入相应的实例，一个SPI接口可能存在多个不同的扩展点实例，这里使用了反射技术和装饰器的设计模式，
     * 修饰这个扩展点实例，需要注意的是，每个扩展点实例内部都存在一个指向同类型的SPI接口的引用
     *
     * @param instance  要被装饰的扩展点实例（SPI接口实现类）
     * @return          返回被装饰后的扩展点实例
     */
    private T injectExtension(T instance) {
        try {
            if (objectFactory != null) {
                for (Method method : instance.getClass().getMethods()) {
                    // 遍历所有set方法，实现自动注入
                    if (method.getName().startsWith("set") && method.getParameterTypes().length == 1 && Modifier.isPublic(method.getModifiers())) {

                        Class<?> pt = method.getParameterTypes()[0];

                        try {
                            String property = method.getName().length() > 3 ? method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4) : "";
                            Object object = objectFactory.getExtension(pt, property);

                            // 通过反射技术注入
                            if (object != null) {
                                method.invoke(instance, object);
                            }
                        } catch (Exception e) {
                            logger.error("fail to inject via method " + method.getName()
                                    + " of interface " + type.getName() + ": " + e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return instance;
    }




    /**
     * This is equivalent to {@code getActivateExtension(url, key, null)}
     *
     * @param url url
     * @param key url parameter key which used to get extension point names
     * @return extension list which are activated.
     * @see #getActivateExtension(com.alibaba.dubbo.common.URL, String, String)
     */
    public List<T> getActivateExtension(URL url, String key) {
        return getActivateExtension(url, key, null);
    }
    /**
     * This is equivalent to {@code getActivateExtension(url, url.getParameter(key).split(","), null)}
     *
     * @param url   url
     * @param key   url parameter key which used to get extension point names
     * @param group group
     * @return extension list which are activated.
     * @see #getActivateExtension(com.alibaba.dubbo.common.URL, String[], String)
     */
    public List<T> getActivateExtension(URL url, String key, String group) {
        String value = url.getParameter(key);
        return getActivateExtension(url, value == null || value.length() == 0 ? null : Constants.COMMA_SPLIT_PATTERN.split(value), group);
    }
    /**
     * Get activate extensions.
     *
     * @param url    url
     * @param values extension point names
     * @param group  group
     * @return extension list which are activated
     * @see com.alibaba.dubbo.common.extension.Activate
     */
    public List<T> getActivateExtension(URL url, String[] values, String group) {
        List<T> exts = new ArrayList<T>();
        List<String> names = values == null ? new ArrayList<String>(0) : Arrays.asList(values);
        if (!names.contains(Constants.REMOVE_VALUE_PREFIX + Constants.DEFAULT_KEY)) {
            getExtensionClasses();
            for (Map.Entry<String, Activate> entry : cachedActivates.entrySet()) {
                String name = entry.getKey();
                Activate activate = entry.getValue();
                if (isMatchGroup(group, activate.group())) {
                    T ext = getExtension(name);
                    if (!names.contains(name)
                            && !names.contains(Constants.REMOVE_VALUE_PREFIX + name)
                            && isActive(activate, url)) {
                        exts.add(ext);
                    }
                }
            }
            Collections.sort(exts, ActivateComparator.COMPARATOR);
        }
        List<T> usrs = new ArrayList<T>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (!name.startsWith(Constants.REMOVE_VALUE_PREFIX)
                    && !names.contains(Constants.REMOVE_VALUE_PREFIX + name)) {
                if (Constants.DEFAULT_KEY.equals(name)) {
                    if (usrs.size() > 0) {
                        exts.addAll(0, usrs);
                        usrs.clear();
                    }
                } else {
                    T ext = getExtension(name);
                    usrs.add(ext);
                }
            }
        }
        if (usrs.size() > 0) {
            exts.addAll(usrs);
        }
        return exts;
    }
    /**
     * This is equivalent to {@code getActivateExtension(url, values, null)}
     *
     * @param url    url
     * @param values extension point names
     * @return extension list which are activated
     * @see #getActivateExtension(com.alibaba.dubbo.common.URL, String[], String)
     */
    public List<T> getActivateExtension(URL url, String[] values) {
        return getActivateExtension(url, values, null);
    }
    private boolean isMatchGroup(String group, String[] groups) {
        if (group == null || group.length() == 0) {
            return true;
        }
        if (groups != null && groups.length > 0) {
            for (String g : groups) {
                if (group.equals(g)) {
                    return true;
                }
            }
        }
        return false;
    }
    private boolean isActive(Activate activate, URL url) {
        String[] keys = activate.value();
        if (keys == null || keys.length == 0) {
            return true;
        }
        for (String key : keys) {
            for (Map.Entry<String, String> entry : url.getParameters().entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                if ((k.equals(key) || k.endsWith("." + key))
                        && ConfigUtils.isNotEmpty(v)) {
                    return true;
                }
            }
        }
        return false;
    }




    @Override
    public String toString() {
        return this.getClass().getName() + "[" + type.getName() + "]";
    }

}