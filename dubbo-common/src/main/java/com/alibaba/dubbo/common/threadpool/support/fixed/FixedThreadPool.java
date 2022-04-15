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
package com.alibaba.dubbo.common.threadpool.support.fixed;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.threadpool.ThreadPool;
import com.alibaba.dubbo.common.threadpool.support.AbortPolicyWithReport;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 创建一个重复使用固定数量线程的线程池
 *
 * @see java.util.concurrent.Executors#newFixedThreadPool(int)
 */
public class FixedThreadPool implements ThreadPool {


    public Executor getExecutor(URL url) {
        String name = url.getParameter(Constants.THREAD_NAME_KEY, Constants.DEFAULT_THREAD_NAME);
        int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
        // 线程池的任务队列大小：
        // 如果为0：则使用同步队列（一手交钱一手交货）；
        // 如果小于0：则使用无界的链阻塞队列；
        // 如果大于0：则使用指定队列大小的有界队列；
        int queues = url.getParameter(Constants.QUEUES_KEY, Constants.DEFAULT_QUEUES);

        return new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS,
                queues == 0 ? new SynchronousQueue<Runnable>() :
                        (queues < 0 ? new LinkedBlockingQueue<Runnable>() : new LinkedBlockingQueue<Runnable>(queues)),
                new NamedThreadFactory(name, true), new AbortPolicyWithReport(name, url));
    }

}