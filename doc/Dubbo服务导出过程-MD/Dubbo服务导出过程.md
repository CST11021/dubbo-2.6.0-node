# Dubbo服务导出过程

## 基本流程

Dubbo服务导出的入口有两个地方：

* 第一个是InitializingBean#afterPropertySet()：当设置了延迟暴露（比如delay=”5000”）时，会立即调用export()方法，并且是通过线程池的方式延迟暴露服务；

* 第二个是Spring 容器发布刷新事件，Dubbo在接收到事件后，会立即执行服务导出逻辑：没有设置延迟暴露或者延迟设置为-1（delay=”-1”）时，dubbo会在Spring实例化完bean之后，在刷新容器最后一步发布ContextRefreshEvent事件的时候通知实现了ApplicationListener的类进行回调onApplicationEvent，dubbo会在这个方法中**立即发布服务，注意此时不是通过线程池的方式发布**。

```xml
dubbo暴露服务有两种情况，一种是设置了延迟暴露（比如delay=”5000”），另外一种是没有设置延迟暴露或者延迟设置为-1（delay=”-1”）：

延迟 5 秒暴露服务
<dubbo:service delay="5000"/>
延迟到 Spring 初始化完成后，再暴露服务
<dubbo:service delay="-1"/>

那什么时候需要设置延迟暴露呢？如果你的服务需要预热时间，比如初始化缓存，等待相关资源就位等，可以使用 delay 进行延迟暴露。对应的配置如下：
```

整个逻辑大致可分为三个部分：

- 第一部分：主要用于检查参数，组装URL。
- 第二部分：导出服务，包含导出服务到本地 (JVM)和导出服务到远程两个过程。
- 第三部分：向注册中心注册服务，用于服务发现。

## 详细流程

### 配置检查

在导出服务之前，Dubbo需要检查用户的配置是否合理，或者为用户补充缺省配置，比如`<dubbo:provider/>`配置：当`<dubbo:service>`配置中没有配置某项参数时，会采用此缺省配置，该标签是可选的；

### 多协议多注册中心

Dubbo允许我们使用不同的协议导出服务，也允许我们向多个注册中心注册服务。暴露服务的时候会根据遍历配置的`<dubbo:protocol/>`信息导出服务，并在导出服务的过程中，将服务注册到注册中心。

**什么时候需要多协议多注册中心？**

多协议导出：由于不同的服务在性能上适用不同协议进行传输，比如大数据用短连接协议，小数据大并发用长连接协议。比如：在使用dubbo过程中碰到了需要上传图片这样的需求，而dubbo协议适合于小数据量大并发的服务调用，像上传文件这种需求推荐使用hessian协议。

```
比如之前hera有提供文件上传的服务，它是通过dubbo协议来调用，将文件上传到oss，然后再将下载路径保存到mysql，这种场景其实可以使用hessian协议来导出服务。
比较好的解决方式：因为dubbo协议适合于小数据量大并发的服务，所以我们只限制文件上传为30M，但是后来产测工具软件做的越来越大有的甚至达到80M的大小，我们就将oss上传的迁移到上层的业务系统，底层hera仅提供mysql的文件路径存储。
```



多注册中心：多注册中心在微服务架构里面是比较常见的，比如，中文站有些服务来不及在青岛部署，只在杭州部署，而青岛的其他应用需要引用此服务，就可以将服务同时注册到两个注册中心。

* **多协议配置示例：**

服务提供者配置：

```xml
<dubbo:registry  protocol="zookeeper"  address="192.168.0.101:2181"/>

<!-- 用dubbo协议在20880端口暴露服务 -->
<dubbo:protocol name="dubbo" port="20880" />  
<!-- 用rmi协议在20881端口暴露服务 -->   
<dubbo:protocol name="rmi" port="20881" />  

<!-- 声明需要暴露的服务接口 -->
<dubbo:service interface="com.test.ServiceDemo" ref="demoService" protocol="dubbo" /> 
<dubbo:service interface="com.test.ServiceDemo2" ref="demoService2" protocol="rmi" />

<!-- 同一服务多协议配置 -->
<dubbo:service interface="com.test.ServiceDemo" ref="demoService" protocol="dubbo,rmi"/>
```

消费者配置：

```xml
<!-- 消费端还是按照正常方式获取服务，只是传递使用的协议不同，底层tcp链接时常不同…… -->
<dubbo:registry protocol="zookeeper" address="192.168.0.101:2181" />    

<dubbo:reference id="demoServicemy" interface="com.test.ServiceDemo"/>
<dubbo:reference id="demoServicemy2" interface="com.test.ServiceDemo2"/>
```

* 多注册中心配置示例：

```xml
<dubbo:registry id="mainRegistry" address="zookeeper://10.18.56.138:2181"/>  
<dubbo:registry id="secondRegistry" address="zookeeper://10.18.56.139:2181" default="false"/>  

<!-- 将服务注册到多个注册中心 -->
<dubbo:service interface="com.dubbo.test.DubboTestApi" version="2.0.0" ref="dubboTestApi" registry="mainRegistry,secondRegistry"/>  
```



### URL组装

配置检查完成后，接下来需要根据这些配置组装URL。

- 首先是将一些信息，比如版本、时间戳、方法名以及各种配置对象的字段信息放入到map中，map 中的内容将作为URL的查询字符串。
- 构建好map后，紧接着是获取上下文路径、主机名以及端口号等信息。
- 最后将map和主机名等数据传给 URL 构造方法创建 URL 对象。



URL 是 Dubbo 配置的载体，通过 URL 可让 Dubbo 的各种配置在各个模块之间传递。URL 之于 Dubbo，犹如水之于鱼，非常重要。大家在阅读 Dubbo 服务导出相关源码的过程中，要注意 URL 内容的变化。

另外，采用 URL 作为配置信息的统一格式，所有扩展点都通过传递 URL 携带配置信息。



### 创建Invoker

Invoker 是由 ProxyFactory 创建而来，Dubbo 默认的 ProxyFactory 实现类是 JavassistProxyFactory。

- JavassistProxyFactory 创建了一个继承自 AbstractProxyInvoker 类的匿名对象，并覆写了抽象方法 doInvoke。覆写后的 doInvoke 逻辑比较简单，仅是将调用请求转发给了 Wrapper 类的 invokeMethod 方法。

- Wrapper 用于“包裹”目标类，Wrapper 是一个抽象类，仅可通过 getWrapper(Class) 方法创建子类。在创建 Wrapper 子类的过程中，子类代码生成逻辑会对 getWrapper 方法传入的 Class 对象进行解析，拿到诸如类方法，类成员变量等信息。以及生成 invokeMethod 方法代码和其他一些方法代码。代码生成完毕后，通过 Javassist 生成 Class 对象，最后再通过反射创建 Wrapper 实例。

- AbstractProxyInvoker 包含一个 Proxy 实例，代理了具体的服务类。

- Proxy 用于代理目标类，Proxy 是一个抽象类，仅可以通过 getProxy(ClassLoader, Class[]) 方法创建子类。可以通过 newInstance(InvocationHandler) 来创建代理实例。

- 服务类 --> Proxy --> Wrapper --> Invokerjava

  

### 导出到本地JVM

- 首先根据 URL 协议头决定是否导出服务。若需导出，则创建一个新的 URL 并将协议头、主机名以及端口设置成新的值。
- 然后创建 Invoker，并调用 InjvmProtocol 的 export 方法导出服务。
- InjvmProtocol 的 export 方法仅创建了一个 InjvmExporter，无其他逻辑。



dubbo服务的本地暴露，显然是针对当服务消费者和服务提供者都在同一个jvm的进程内这种场景 。通常是发生在服务之间的调用的情况下。一种情况就是A服务调用B服务的情况，如果A服务和B服务都是在一个线程中进行服务暴露的，就是本地调用。



### 导出到远程

- 源码：[dubbo-registry-api] RegistryProtocol#export
- 调用 doLocalExport 导出服务，调用 Protocol 的 export 方法
- 向注册中心注册服务
- 向注册中心进行订阅 override 数据
- 创建并返回 DestroyableExporter

### dubbo协议导出服务

默认使用dubbo协议导出服务。

- 源码：[dubbo-rpc-dubbo] DubboProtocol#export
- 源码：[dubbo-rpc-dubbo] DubboProtocol#openServer
- 源码：[dubbo-rpc-dubbo] DubboProtocol#createServer
- createServer 包含三个核心的逻辑。第一是检测是否存在 server 参数所代表的 Transporter 拓展，不存在则抛出异常。第二是创建服务器实例。第三是检测是否支持 client 参数所表示的 Transporter 拓展，不存在也是抛出异常。

### 启动服务器

默认使用Netty实现服务器。

- 源码：[dubbo-remoting-api] HeaderExchanger#bind
- getTransporter() 方法获取的 Transporter 是在运行时动态创建的，类名为 TransporterAdaptive，也就是自适应拓展类。TransporterAdaptive 会在运行时根据传入的 URL 参数决定加载什么类型的 Transporter，默认为 NettyTransporter。
- 源码：[dubbo-remoting-netty] NettyTransporter#bind
- 源码：[dubbo-remoting-netty] NettyServer#doOpen

### 注册到注册中心

注册中心默认为Zookeeper，客户端默认使用Curator。

- 源码：[dubbo-registry-zookeeper] ZookeeperRegistryFactory#createRegistry
- 源码：[dubbo-remoting-zookeeper] CuratorZookeeperTransporter#createZookeeperClient





服务导出伪代码

```java
public void export() {
	
	// 1、检查必要配置有配置，比如应用端口、注册中心、协议等配置，以及<dubbo:provider/>默认的服务参数配置
	checkConfig();

	// 2、将导出的服务相关配置组装为URL对象，注意这里的url为多个，因为可能存在多个注册中心
	List<URL> urls = loadRegistries();

	// 3、遍历所有协议进行服务导出，dubbo支持多协议多注册中心
	for (ProtocolConfig protocol : protocols) {
		doExport(protocol, urls);
	}
  
}


pulbic void doExport(protocol, urls) {
	
	// 对每个url进行服务导出
	for (URL url : urls) {

		// 使用代理工厂类获取Invoker对象
		Invoker<?> invoker = proxyFactory.getInvoker(serviceImpl, interfaceClass, url);

		// 使用通信协议导出服务
		Exporter<?> exporter = protocolExport(invoker);

		// 使用注册中心导出服务
		register(invoker);

		// 将Exporter保存到内存
		addExporter(exporter);
	}

}

public Exporter protocolExport(invoker) {

	// 1、创建服务key，例如：com.alibaba.dubbo.demo.DemoService:20880
	String key = serviceKey(url);

	// 2、创建Exporter
	Exporter exporter = buildExporter(key, invoker);

	// 3、启动服务，这样就可以监听来自客户端的调用请求了
	createServer(url)

}
```

