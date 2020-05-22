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
package com.alibaba.dubbo.config;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.config.support.Parameter;
import com.alibaba.dubbo.rpc.ExporterListener;

import java.util.Arrays;
import java.util.List;

/**
 * AbstractServiceConfig
 *
 * @export
 */
public abstract class AbstractServiceConfig extends AbstractInterfaceConfig {

    private static final long serialVersionUID = 1L;

    /** 表示服务的version */
    protected String version;
    /** 表示服务的group */
    protected String group;
    /** 表示服务是否已弃用 */
    protected Boolean deprecated;
    /**
     * 设置延迟服务发布的时间
     * dubbo暴露服务有两种情况，一种是设置了延迟暴露（比如delay=”5000”），另外一种是没有设置延迟暴露或者延迟设置为-1（delay=”-1”）：
     *
     *     设置了延迟暴露，dubbo在Spring实例化bean（initializeBean）的时候会对实现了InitializingBean的类进行回调，回调方法是
     * afterPropertySet()，如果设置了延迟暴露，dubbo在这个方法中进行服务的发布。
     *     没有设置延迟或者延迟为-1，dubbo会在Spring实例化完bean之后，在刷新容器最后一步发布ContextRefreshEvent事件的时候，
     * 通知实现了ApplicationListener的类进行回调onApplicationEvent，dubbo会在这个方法中发布服务。
     *
     * 使用export初始化的时候会将Bean对象转换成URL格式，所有Bean属性转换成URL的参数。
     *
     * 一般情况下，如果你的服务需要预热时间，比如初始化缓存，等待相关资源就位等，可以使用 delay 进行延迟暴露，例如：
     * ① 延迟 5 秒暴露服务：<dubbo:service delay="5000" />
     * ② 延迟到 Spring 初始化完成后，再暴露服务：<dubbo:service delay="-1" />
     */
    protected Integer delay;
    /** 表示是否已经暴露服务 */
    protected Boolean export;
    /** 服务权重 */
    protected Integer weight;
    /** 文档中心 */
    protected String document;
    /** 是否在注册中心注册为动态服务 */
    protected Boolean dynamic;
    /** 是否使用令牌 */
    protected String token;
    /** 默认：false，设为true，将向logger中输出访问日志，也可填写访问日志文件路径，直接把访问日志输出到指定文件 */
    protected String accesslog;
    /** 一个服务可以支持多协议导出 */
    protected List<ProtocolConfig> protocols;
    /** 默认：0，服务提供者每服务每方法最大可并行执行请求数 */
    private Integer executes;
    // whether to register
    private Boolean register;
    // warm up period
    private Integer warmup;
    /** dubbo协议缺省为hessian2，rmi协议缺省为java，http协议缺省为json，协议序列化方式，当协议支持多种序列化方式时使用，比如：dubbo协议的dubbo,hessian2,java,compactedjava，以及http协议的json,xml等 */
    private String serialization;




    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        checkKey("version", version);
        this.version = version;
    }
    public String getGroup() {
        return group;
    }
    public void setGroup(String group) {
        checkKey("group", group);
        this.group = group;
    }
    public Integer getDelay() {
        return delay;
    }
    public void setDelay(Integer delay) {
        this.delay = delay;
    }
    public Boolean getExport() {
        return export;
    }
    public void setExport(Boolean export) {
        this.export = export;
    }
    public Integer getWeight() {
        return weight;
    }
    public void setWeight(Integer weight) {
        this.weight = weight;
    }
    @Parameter(escaped = true)
    public String getDocument() {
        return document;
    }
    public void setDocument(String document) {
        this.document = document;
    }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        checkName("token", token);
        this.token = token;
    }
    public void setToken(Boolean token) {
        if (token == null) {
            setToken((String) null);
        } else {
            setToken(String.valueOf(token));
        }
    }
    public Boolean isDeprecated() {
        return deprecated;
    }
    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }
    public Boolean isDynamic() {
        return dynamic;
    }
    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }
    public List<ProtocolConfig> getProtocols() {
        return protocols;
    }
    @SuppressWarnings({"unchecked"})
    public void setProtocols(List<? extends ProtocolConfig> protocols) {
        this.protocols = (List<ProtocolConfig>) protocols;
    }
    public ProtocolConfig getProtocol() {
        return protocols == null || protocols.size() == 0 ? null : protocols.get(0);
    }
    public void setProtocol(ProtocolConfig protocol) {
        this.protocols = Arrays.asList(new ProtocolConfig[]{protocol});
    }
    public String getAccesslog() {
        return accesslog;
    }
    public void setAccesslog(String accesslog) {
        this.accesslog = accesslog;
    }
    public void setAccesslog(Boolean accesslog) {
        if (accesslog == null) {
            setAccesslog((String) null);
        } else {
            setAccesslog(String.valueOf(accesslog));
        }
    }
    public Integer getExecutes() {
        return executes;
    }
    public void setExecutes(Integer executes) {
        this.executes = executes;
    }
    @Parameter(key = Constants.SERVICE_FILTER_KEY, append = true)
    public String getFilter() {
        return super.getFilter();
    }
    @Parameter(key = Constants.EXPORTER_LISTENER_KEY, append = true)
    public String getListener() {
        return super.getListener();
    }
    @Override
    public void setListener(String listener) {
        checkMultiExtension(ExporterListener.class, "listener", listener);
        super.setListener(listener);
    }
    public Boolean isRegister() {
        return register;
    }
    public void setRegister(Boolean register) {
        this.register = register;
    }
    public Integer getWarmup() {
        return warmup;
    }
    public void setWarmup(Integer warmup) {
        this.warmup = warmup;
    }
    public String getSerialization() {
        return serialization;
    }
    public void setSerialization(String serialization) {
        this.serialization = serialization;
    }

}