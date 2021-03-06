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
package com.alibaba.dubbo.container;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConfigUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main. (API, Static, ThreadSafe)
 *
 容器的停机
    Dubbo是通过JDK的ShutdownHook来完成优雅停机的，所以如果用户使用”kill -9 PID”等强制关闭指令，是不会执行优雅停机的，只有通过”kill PID”时，才会执行。
 此时有下面的情况：

 1，服务提供方
    停止时，先标记为不接收新请求，新请求过来时直接报错，让客户端重试其它机器。
    然后，检测线程池中的线程是否正在运行，如果有，等待所有线程执行完成，除非超时，则强制关闭。

 2，服务消费方
    停止时，不再发起新的调用请求，所有新的调用在客户端即报错。
    然后，检测有没有请求的响应还没有返回，等待响应返回，除非超时，则强制关闭。
 */
public class Main {

    public static final String CONTAINER_KEY = "dubbo.container";
    public static final String SHUTDOWN_HOOK_KEY = "dubbo.shutdown.hook";
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final ExtensionLoader<Container> loader = ExtensionLoader.getExtensionLoader(Container.class);
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final Condition STOP = LOCK.newCondition();

    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                String config = ConfigUtils.getProperty(CONTAINER_KEY, loader.getDefaultExtensionName());
                // 将","前后的空格剔除，并以","分割，例如：Constants.COMMA_SPLIT_PATTERN.split(" 123,abc , abc123 ") -> [" 123", "abc", "abc123 "]
                args = Constants.COMMA_SPLIT_PATTERN.split(config);
            }

            final List<Container> containers = new ArrayList<Container>();
            for (int i = 0; i < args.length; i++) {
                containers.add(loader.getExtension(args[i]));
            }
            logger.info("Use container type(" + Arrays.toString(args) + ") to run dubbo serivce.");

            // 添加一个钩子，以优雅的方式停止
            if ("true".equals(System.getProperty(SHUTDOWN_HOOK_KEY))) {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        for (Container container : containers) {
                            try {
                                container.stop();
                                logger.info("Dubbo " + container.getClass().getSimpleName() + " stopped!");
                            } catch (Throwable t) {
                                logger.error(t.getMessage(), t);
                            }

                            try {
                                LOCK.lock();
                                STOP.signal();
                            } finally {
                                LOCK.unlock();
                            }

                        }
                    }
                });
            }

            // 根据参数启动容器，我们在启动容器时候，可以选择spring、jetty、log4j、logback等参数
            for (Container container : containers) {
                container.start();
                logger.info("Dubbo " + container.getClass().getSimpleName() + " started!");
            }
            System.out.println(new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]").format(new Date()) + " Dubbo service server started!");
        } catch (RuntimeException e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            System.exit(1);
        }

        // 等钩子里的容器优雅停止后，会释放锁，这里再结束线程
        try {
            LOCK.lock();
            STOP.await();
        } catch (InterruptedException e) {
            logger.warn("Dubbo service server stopped, interrupted by other thread!", e);
        } finally {
            LOCK.unlock();
        }

    }

}