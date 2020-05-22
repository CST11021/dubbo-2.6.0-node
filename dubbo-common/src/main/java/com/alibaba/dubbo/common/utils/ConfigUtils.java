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
package com.alibaba.dubbo.common.utils;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);
    private static Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\s*\\{?\\s*([\\._0-9a-zA-Z]+)\\s*\\}?");
    /** 用于缓存 dubbo.properties 的文件配置项 */
    private static volatile Properties PROPERTIES;
    private static int PID = -1;

    private ConfigUtils() {
    }

    /**
     * 检查该值是不为空，当value为false、0、null或N/A时，返回true
     *
     * @param value
     * @return
     */
    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    /**
     * 检查该值是否为空，当value为false、0、null或N/A时，返回true
     *
     * @param value
     * @return
     */
    public static boolean isEmpty(String value) {
        return value == null || value.length() == 0
                || "false".equalsIgnoreCase(value)
                || "0".equalsIgnoreCase(value)
                || "null".equalsIgnoreCase(value)
                || "N/A".equalsIgnoreCase(value);
    }

    /**
     * 当value为true或default时，返回true
     *
     * @param value
     * @return
     */
    public static boolean isDefault(String value) {
        return "true".equalsIgnoreCase(value)
                || "default".equalsIgnoreCase(value);
    }

    /**
     * 将默认扩展名插入扩展名列表。
     *
     * 扩展列表支持
     * 特殊值：default, 表示默认扩展名的位置。
     * 特殊符号：-，意味着移除. 例如1：-foo1，将删除默认扩展名'foo'; 例如2：-default，将删除所有默认扩展名。
     *
     * @param type Extension type
     * @param cfg  Extension name list
     * @param def  Default extension list
     * @return result extension list
     */
    public static List<String> mergeValues(Class<?> type, String cfg, List<String> def) {
        List<String> defaults = new ArrayList<String>();
        if (def != null) {
            for (String name : def) {
                if (ExtensionLoader.getExtensionLoader(type).hasExtension(name)) {
                    defaults.add(name);
                }
            }
        }

        List<String> names = new ArrayList<String>();

        // add initial values
        String[] configs = (cfg == null || cfg.trim().length() == 0) ? new String[0] : Constants.COMMA_SPLIT_PATTERN.split(cfg);
        for (String config : configs) {
            if (config != null && config.trim().length() > 0) {
                names.add(config);
            }
        }

        // -default is not included
        if (!names.contains(Constants.REMOVE_VALUE_PREFIX + Constants.DEFAULT_KEY)) {
            // add default extension
            int i = names.indexOf(Constants.DEFAULT_KEY);
            if (i > 0) {
                names.addAll(i, defaults);
            } else {
                names.addAll(0, defaults);
            }
            names.remove(Constants.DEFAULT_KEY);
        } else {
            names.remove(Constants.DEFAULT_KEY);
        }

        // merge - configuration
        for (String name : new ArrayList<String>(names)) {
            if (name.startsWith(Constants.REMOVE_VALUE_PREFIX)) {
                names.remove(name);
                names.remove(name.substring(1));
            }
        }
        return names;
    }

    public static String replaceProperty(String expression, Map<String, String> params) {
        if (expression == null || expression.length() == 0 || expression.indexOf('$') < 0) {
            return expression;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = System.getProperty(key);
            if (value == null && params != null) {
                value = params.get(key);
            }
            if (value == null) {
                value = "";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 加载 dubbo.properties 文件配置项
     *
     *
     * Java提供了System类的静态方法getenv()和getProperty()用于返回系统相关的变量与属性，getenv方法返回的变量大多与系统相关，getProperty方法返回的变量大多与java程序有关，例如：
     *
     * System.getenv()
     *
     * USERPROFILE        ：用户目录
     * USERDNSDOMAIN      ：用户域
     * PATHEXT            ：可执行后缀
     * JAVA_HOME          ：Java安装目录
     * TEMP               ：用户临时文件目录
     * SystemDrive        ：系统盘符
     * ProgramFiles       ：默认程序目录
     * USERDOMAIN         ：帐户的域的名称
     * ALLUSERSPROFILE    ：用户公共目录
     * SESSIONNAME        ：Session名称
     * TMP                ：临时目录
     * Path               ：path环境变量
     * CLASSPATH          ：classpath环境变量
     * PROCESSOR_ARCHITECTURE ：处理器体系结构
     * OS                     ：操作系统类型
     * PROCESSOR_LEVEL    ：处理级别
     * COMPUTERNAME       ：计算机名
     * Windir             ：系统安装目录
     * SystemRoot         ：系统启动目录
     * USERNAME           ：用户名
     * ComSpec            ：命令行解释器可执行程序的准确路径
     * APPDATA            ：应用程序数据目录
     *
     *
     *
     * System.getProperty()
     *
     * java.version Java ：运行时环境版本
     * java.vendor Java ：运行时环境供应商
     * java.vendor.url ：Java供应商的 URL
     * java.home &nbsp;&nbsp;：Java安装目录
     * java.vm.specification.version： Java虚拟机规范版本
     * java.vm.specification.vendor ：Java虚拟机规范供应商
     * java.vm.specification.name &nbsp; ：Java虚拟机规范名称
     * java.vm.version ：Java虚拟机实现版本
     * java.vm.vendor ：Java虚拟机实现供应商
     * java.vm.name&nbsp; ：Java虚拟机实现名称
     * java.specification.version：Java运行时环境规范版本
     * java.specification.vendor：Java运行时环境规范供应商
     * java.specification.name ：Java运行时环境规范名称
     * java.class.version ：Java类格式版本号
     * java.class.path ：Java类路径
     * java.library.path  ：加载库时搜索的路径列表
     * java.io.tmpdir  ：默认的临时文件路径
     * java.compiler  ：要使用的 JIT编译器的名称
     * java.ext.dirs ：一个或多个扩展目录的路径
     * os.name ：操作系统的名称
     * os.arch  ：操作系统的架构
     * os.version  ：操作系统的版本
     * file.separator ：文件分隔符
     * path.separator ：路径分隔符
     * line.separator ：行分隔符
     * user.name ：用户的账户名称
     * user.home ：用户的主目录
     * user.dir：用户的当前工作目录
     *
     * @return
     */
    public static Properties getProperties() {
        if (PROPERTIES == null) {
            synchronized (ConfigUtils.class) {
                if (PROPERTIES == null) {
                    String path = System.getProperty(Constants.DUBBO_PROPERTIES_KEY);
                    if (path == null || path.length() == 0) {
                        path = System.getenv(Constants.DUBBO_PROPERTIES_KEY);
                        if (path == null || path.length() == 0) {
                            path = Constants.DEFAULT_DUBBO_PROPERTIES;
                        }
                    }
                    PROPERTIES = ConfigUtils.loadProperties(path, false, true);
                }
            }
        }
        return PROPERTIES;
    }

    public static void setProperties(Properties properties) {
        if (properties != null) {
            PROPERTIES = properties;
        }
    }

    public static void addProperties(Properties properties) {
        if (properties != null) {
            getProperties().putAll(properties);
        }
    }

    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static String getProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value != null && value.length() > 0) {
            return value;
        }
        Properties properties = getProperties();
        return replaceProperty(properties.getProperty(key, defaultValue), (Map) properties);
    }

    /**
     * System environment -> System properties
     *
     * @param key key
     * @return value
     */
    public static String getSystemProperty(String key) {
        String value = System.getenv(key);
        if (value == null || value.length() == 0) {
            value = System.getProperty(key);
        }
        return value;
    }

    public static Properties loadProperties(String fileName) {
        return loadProperties(fileName, false, false);
    }

    public static Properties loadProperties(String fileName, boolean allowMultiFile) {
        return loadProperties(fileName, allowMultiFile, false);
    }

    /**
     * Load properties file to {@link Properties} from class path.
     *
     * @param fileName       properties file name. for example: <code>dubbo.properties</code>, <code>METE-INF/conf/foo.properties</code>
     * @param allowMultiFile if <code>false</code>, throw {@link IllegalStateException} when found multi file on the class path.
     * @param optional       is optional. if <code>false</code>, log warn when properties config file not found!s
     * @return loaded {@link Properties} content. <ul>
     * <li>return empty Properties if no file found.
     * <li>merge multi properties file if found multi file
     * </ul>
     * @throws IllegalStateException not allow multi-file, but multi-file exsit on class path.
     */
    public static Properties loadProperties(String fileName, boolean allowMultiFile, boolean optional) {
        Properties properties = new Properties();
        if (fileName.startsWith("/")) {
            try {
                FileInputStream input = new FileInputStream(fileName);
                try {
                    properties.load(input);
                } finally {
                    input.close();
                }
            } catch (Throwable e) {
                logger.warn("Failed to load " + fileName + " file from " + fileName + "(ingore this file): " + e.getMessage(), e);
            }
            return properties;
        }

        List<java.net.URL> list = new ArrayList<java.net.URL>();
        try {
            Enumeration<java.net.URL> urls = ClassHelper.getClassLoader().getResources(fileName);
            list = new ArrayList<java.net.URL>();
            while (urls.hasMoreElements()) {
                list.add(urls.nextElement());
            }
        } catch (Throwable t) {
            logger.warn("Fail to load " + fileName + " file: " + t.getMessage(), t);
        }

        if (list.size() == 0) {
            if (!optional) {
                logger.warn("No " + fileName + " found on the class path.");
            }
            return properties;
        }

        if (!allowMultiFile) {
            if (list.size() > 1) {
                String errMsg = String.format("only 1 %s file is expected, but %d dubbo.properties files found on class path: %s",
                        fileName, list.size(), list.toString());
                logger.warn(errMsg);
                // throw new IllegalStateException(errMsg); // see http://code.alibabatech.com/jira/browse/DUBBO-133
            }

            // fall back to use method getResourceAsStream
            try {
                properties.load(ClassHelper.getClassLoader().getResourceAsStream(fileName));
            } catch (Throwable e) {
                logger.warn("Failed to load " + fileName + " file from " + fileName + "(ingore this file): " + e.getMessage(), e);
            }
            return properties;
        }

        logger.info("load " + fileName + " properties file from " + list);

        for (java.net.URL url : list) {
            try {
                Properties p = new Properties();
                InputStream input = url.openStream();
                if (input != null) {
                    try {
                        p.load(input);
                        properties.putAll(p);
                    } finally {
                        try {
                            input.close();
                        } catch (Throwable t) {
                        }
                    }
                }
            } catch (Throwable e) {
                logger.warn("Fail to load " + fileName + " file from " + url + "(ingore this file): " + e.getMessage(), e);
            }
        }

        return properties;
    }

    /** 获取当前进程的pid */
    public static int getPid() {
        if (PID < 0) {
            try {
                RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
                String name = runtime.getName(); // format: "pid@hostname"
                PID = Integer.parseInt(name.substring(0, name.indexOf('@')));
            } catch (Throwable e) {
                PID = 0;
            }
        }
        return PID;
    }

}