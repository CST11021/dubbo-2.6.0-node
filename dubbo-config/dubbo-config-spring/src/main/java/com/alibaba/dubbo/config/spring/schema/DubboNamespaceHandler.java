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
package com.alibaba.dubbo.config.spring.schema;

import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.config.*;
import com.alibaba.dubbo.config.spring.AnnotationBean;
import com.alibaba.dubbo.config.spring.ReferenceBean;
import com.alibaba.dubbo.config.spring.ServiceBean;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * DubboNamespaceHandler
 *
 * @export
 */
public class DubboNamespaceHandler extends NamespaceHandlerSupport {

    static {
        Version.checkDuplicate(DubboNamespaceHandler.class);
    }

    /**
        从这里也可以看到，对应的支持的标签其实不多。所有的 Parser 都封装到了DubboBeanDefinitionParser 中。对应的 class，
     就是传入的 beanClass。比如 application 的就是ApplicationConfig。module 的就是 ModuleConfig。经过 Parser 的转换，
     provider.xml 大概可以变成如下的样子(具体的解析不多解释了)

        <bean id="hello-world-app" class="com.alibaba.dubbo.config.ApplicationConfig"/>

        <bean id="registryConfig" class="com.alibaba.dubbo.config.RegistryConfig">
            <property name="address" value="10.125.195.174:2181"/>
            <property name="protocol" value="zookeeper"/>
        </bean>

        <bean id="dubbo" class="com.alibaba.dubbo.config.ProtocolConfig">
            <property name="port" value="20880"/>
        </bean>

        <bean id="demo.service.DemoService" class="com.alibaba.dubbo.config.spring.ServiceBean">
            <property name="interface" value="demo.service.DemoService"/>
            <property name="ref" ref="demoService"/>
        </bean>

        <bean id="demoService" class="demo.service.DemoServiceImpl" />
     实际上在生成 bean 的过程主要是把这些属性都梳理清楚，生成对应的类；
     */
    public void init() {
        //配置<dubbo:application>标签解析器
        registerBeanDefinitionParser("application", new DubboBeanDefinitionParser(ApplicationConfig.class, true));
        //配置<dubbo:module>标签解析器
        registerBeanDefinitionParser("module", new DubboBeanDefinitionParser(ModuleConfig.class, true));
        //配置<dubbo:registry>标签解析器
        registerBeanDefinitionParser("registry", new DubboBeanDefinitionParser(RegistryConfig.class, true));
        //配置<dubbo:monitor>标签解析器
        registerBeanDefinitionParser("monitor", new DubboBeanDefinitionParser(MonitorConfig.class, true));
        //配置<dubbo:provider>标签解析器
        registerBeanDefinitionParser("provider", new DubboBeanDefinitionParser(ProviderConfig.class, true));
        //配置<dubbo:consumer>标签解析器
        registerBeanDefinitionParser("consumer", new DubboBeanDefinitionParser(ConsumerConfig.class, true));
        //配置<dubbo:protocol>标签解析器
        registerBeanDefinitionParser("protocol", new DubboBeanDefinitionParser(ProtocolConfig.class, true));
        //配置<dubbo:service>标签解析器
        registerBeanDefinitionParser("service", new DubboBeanDefinitionParser(ServiceBean.class, true));
        //配置<dubbo:refenrence>标签解析器
        registerBeanDefinitionParser("reference", new DubboBeanDefinitionParser(ReferenceBean.class, false));
        //配置<dubbo:annotation>标签解析器
        registerBeanDefinitionParser("annotation", new DubboBeanDefinitionParser(AnnotationBean.class, true));
    }

}