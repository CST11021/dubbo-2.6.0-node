##Dubbo的SPI机制

Dubbo的SPI机制官方文档：

https://dubbo.apache.org/zh-cn/docs/source_code_guide/dubbo-spi.html

https://dubbo.apache.org/zh-cn/docs/source_code_guide/adaptive-extension.html

###1.简介

​		SPI 全称为 Service Provider Interface，是一种服务发现机制。SPI 的本质是将接口实现类的全限定名配置在文件中，并由服务加载器读取配置文件，加载实现类。这样可以在运行时，动态为接口替换实现类。正因此特性，我们可以很容易的通过 SPI 机制为我们的程序提供拓展功能。SPI 机制在第三方框架中也有所应用，比如 Dubbo 就是通过 SPI 机制加载所有的组件。不过，Dubbo 并未使用 Java 原生的 SPI 机制，而是对其进行了增强，使其能够更好的满足需求。在 Dubbo 中，SPI 是一个非常重要的模块。基于 SPI，我们可以很容易的对 Dubbo 进行拓展。如果大家想要学习 Dubbo 的源码，SPI 机制务必弄懂。接下来，我们先来了解一下 Java SPI 与 Dubbo SPI 的用法，然后再来分析 Dubbo SPI 的源码。



**SPI机制的作用**

​		使用Java SPI机制的优势是实现解耦，使得第三方服务模块的装配控制的逻辑与调用者的业务代码分离，而不是耦合在一起。应用程序可以根据实际业务情况启用框架扩展或替换框架组件。




###2.SPI 示例

####2.1 Java SPI 示例

前面简单介绍了 SPI 机制的原理，本节通过一个示例演示 Java SPI 的使用方法。使用Java SPI，需要遵循如下约定：

1. 服务提供者提供了接口的一种具体实现后，在jar包的META-INF/services目录下创建一个以“接口全限定名”为命名的文件，内容为实现类的全限定名；
2. 接口实现类所在的jar包放在主程序的classpath中；
3. 主程序通过java.util.ServiceLoder动态装载实现模块，它通过扫描META-INF/services目录下的配置文件找到实现类的全限定名，把类加载到JVM；
4. SPI的实现类必须携带一个不带参数的构造方法；

首先，我们定义一个接口，名称为 Robot。

```java
public interface Robot {
    void sayHello();
}
```

接下来定义两个实现类，分别为 OptimusPrime 和 Bumblebee。

```java
public class OptimusPrime implements Robot {
    
    @Override
    public void sayHello() {
        System.out.println("Hello, I am Optimus Prime.");
    }
}

public class Bumblebee implements Robot {

    @Override
    public void sayHello() {
        System.out.println("Hello, I am Bumblebee.");
    }
}	
```

接下来 META-INF/services 文件夹下创建一个文件，名称为 Robot 的全限定名 org.apache.spi.Robot。文件内容为实现类的全限定的类名，如下：

```
org.apache.spi.OptimusPrime
org.apache.spi.Bumblebee
```

做好所需的准备工作，接下来编写代码进行测试。

```java
public class JavaSPITest {

    @Test
    public void sayHello() throws Exception {
        ServiceLoader<Robot> serviceLoader = ServiceLoader.load(Robot.class);
        System.out.println("Java SPI");
        serviceLoader.forEach(Robot::sayHello);
    }
}
```

最后来看一下测试结果，如下：

<img src="assets/image-20190915094826365.png"/>

从测试结果可以看出，我们的两个实现类被成功的加载，并输出了相应的内容。关于 Java SPI 的演示先到这里，接下来演示 Dubbo SPI。

####2.2 Dubbo SPI 示例

Dubbo 并未使用 Java SPI，而是重新实现了一套功能更强的 SPI 机制。Dubbo SPI 的相关逻辑被封装在了 ExtensionLoader 类中，通过 ExtensionLoader，我们可以加载指定的实现类。Dubbo SPI 所需的配置文件需放置在 META-INF/dubbo 路径下，配置内容如下。

```
optimusPrime = org.apache.spi.OptimusPrime
bumblebee = org.apache.spi.Bumblebee
```

与 Java SPI 实现类配置不同，Dubbo SPI 是通过键值对的方式进行配置，这样我们可以按需加载指定的实现类。另外，在测试 Dubbo SPI 时，需要在 Robot 接口上标注 @SPI 注解。下面来演示 Dubbo SPI 的用法：

```java
public class DubboSPITest {

    @Test
    public void sayHello() throws Exception {
        ExtensionLoader<Robot> extensionLoader = 
            ExtensionLoader.getExtensionLoader(Robot.class);
        Robot optimusPrime = extensionLoader.getExtension("optimusPrime");
        optimusPrime.sayHello();
        Robot bumblebee = extensionLoader.getExtension("bumblebee");
        bumblebee.sayHello();
    }
}
```

测试结果如下：

<img src="assets/image-20190915094944067.png"/>



#### Java和Dubbo SPI两者区别

Dubbo改进了JDK标准的SPI的一下问题：

- JDK 标准的 SPI 会一次性实例化扩展点所有实现，如果有扩展实现初始化很耗时，但如果没用上也加载，会很浪费资源。
- dubbo spi增加了对扩展点 IoC 和 AOP 的支持，一个扩展点可以直接 setter 注入其它扩展点。
- 如果扩展点加载失败，连扩展点的名称都拿不到了。比如：JDK 标准的 ScriptEngine，通过 getName() 获取脚本类型的名称，但如果 RubyScriptEngine 因为所依赖的 jruby.jar 不存在，导致 RubyScriptEngine 类加载失败，这个失败原因被吃掉了，和 ruby 对应不起来，当用户执行 ruby 脚本时，会报不支持 ruby，而不是真正失败的原因。
- JDK的spi是通过System ClassLoader，Dubbo spi是通过Extend ClassLoader





###3.自适应扩展机制

​		在 Dubbo 中，很多拓展都是通过 SPI 机制进行加载的，比如 Protocol、Cluster、LoadBalance 等。有时，有些拓展并不想在框架启动阶段被加载，而是希望在拓展方法被调用时，根据运行时参数进行加载。这听起来有些矛盾。拓展未被加载，那么拓展方法就无法被调用（静态方法除外）。拓展方法未被调用，拓展就无法被加载。对于这个矛盾的问题，Dubbo 通过自适应拓展机制很好的解决了。自适应拓展机制的实现逻辑比较复杂，首先 Dubbo 会为拓展接口生成具有代理功能的代码。然后通过 javassist 或 jdk 编译这段代码，得到 Class 类。最后再通过反射创建代理类，整个过程比较复杂。

​		为了让大家对自适应拓展有一个感性的认识，下面我们通过一个示例进行演示。这是一个与汽车相关的例子，我们有一个车轮制造厂接口

WheelMaker：

```java
public interface WheelMaker {
    Wheel makeWheel(URL url);
}
```

WheelMaker 接口的自适应实现类如下：

```java
public class AdaptiveWheelMaker implements WheelMaker {
    public Wheel makeWheel(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        
    	// 1.从 URL 中获取 WheelMaker 名称
        String wheelMakerName = url.getParameter("Wheel.maker");
        if (wheelMakerName == null) {
            throw new IllegalArgumentException("wheelMakerName == null");
        }
        
        // 2.通过 SPI 加载具体的 WheelMaker
        WheelMaker wheelMaker = ExtensionLoader
            .getExtensionLoader(WheelMaker.class).getExtension(wheelMakerName);
        
        // 3.调用目标方法
        return wheelMaker.makeWheel(URL url);
    }
}
```

AdaptiveWheelMaker 是一个代理类，与传统的代理逻辑不同，AdaptiveWheelMaker 所代理的对象是在 makeWheel 方法中通过 SPI 加载得到的。makeWheel 方法主要做了三件事情：

1. 从 URL 中获取 WheelMaker 名称
2. 通过 SPI 加载具体的 WheelMaker 实现类
3. 调用目标方法

接下来，我们来看看汽车制造厂 CarMaker 接口与其实现类。

```java
public interface CarMaker {
    Car makeCar(URL url);
}

public class RaceCarMaker implements CarMaker {
    WheelMaker wheelMaker;
 
    // 通过 setter 注入 AdaptiveWheelMaker
    public setWheelMaker(WheelMaker wheelMaker) {
        this.wheelMaker = wheelMaker;
    }
 
    public Car makeCar(URL url) {
        Wheel wheel = wheelMaker.makeWheel(url);
        return new RaceCar(wheel, ...);
    }
}
```

RaceCarMaker 持有一个 WheelMaker 类型的成员变量，在程序启动时，我们可以将 AdaptiveWheelMaker 通过 setter 方法注入到 RaceCarMaker 中。在运行时，假设有这样一个 url 参数传入：

```url
dubbo://192.168.0.101:20880/XxxService?wheel.maker=MichelinWheelMaker
```

RaceCarMaker 的 makeCar 方法将上面的 url 作为参数传给 AdaptiveWheelMaker 的 makeWheel 方法，makeWheel 方法从 url 中提取 wheel.maker 参数，得到 MichelinWheelMaker。之后再通过 SPI 加载配置名为 MichelinWheelMaker 的实现类，得到具体的 WheelMaker 实例。

上面的示例展示了自适应拓展类的核心实现 ---- 在拓展接口的方法被调用时，通过 SPI 加载具体的拓展实现类，并调用拓展对象的同名方法。接下来，我们深入到源码中，探索自适应拓展类生成的过程。

###4.@Adaptive注解

引用dubbo官方文档的一段话：

 Adaptive 可注解在类或方法上。当 Adaptive 注解在类上时，Dubbo 不会为该类生成代理类。注解在方法（接口方法）上时，Dubbo 则会为该方法生成代理逻辑。Adaptive 注解在类上的情况很少，在 Dubbo 中，仅有两个类被 Adaptive 注解了，分别是 AdaptiveCompiler 和 AdaptiveExtensionFactory。此种情况，表示拓展的加载逻辑由人工编码完成。更多时候，Adaptive 是注解在接口方法上的，表示拓展的加载逻辑需由框架自动生成。



**为什么要设计adaptive？注解在类上和注解在方法上的区别？**

 adaptive设计的目的是为了识别固定已知类和扩展未知类。

1. 注解在类上：代表人工实现，实现一个装饰类（设计模式中的装饰模式），它主要作用于固定已知类，

   目前整个系统只有2个， AdaptiveCompiler，AdaptiveExtensionFactory

   - 为什么AdaptiveCompiler这个类是固定已知的？因为整个框架仅支持Javassist和JdkCompiler。
   - 为什么AdaptiveExtensionFactory这个类是固定已知的？因为整个框架仅支持2个objFactory,一个是spi,另一个是spring

2. 注解在方法上：代表自动生成和编译一个动态的Adpative类，它主要是用于SPI，因为spi的类是不固定、未知的扩展类，所以设计了动态$Adaptive类.
   例如 Protocol的spi类有 injvm dubbo registry filter listener等等 很多扩展未知类，
   它设计了Protocol$Adaptive的类，通过ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(spi类);来提取对象

####注解在接口上

​		在接口上，在调用getAdaptiveExtension方法时，直接返回该类（不会去动态生成代理类），然后执行IOC；

####注解在方法上

**在方法上会生成代理代理类**

关于createAdaptiveExtensionClassCode方法

1. 至少有一个方法被@Adaptive修饰

2. 被@Adaptive修饰得方法得参数 必须满足参数中有一个是URL类型，或者有至少一个参数有一个“公共且非静态的返回URL的无参get方法”

3. @Adaptive注解中的值这里我叫它value，value可以是一个数组，如果为空的话，vlaue等于接口名小写(例如接口名:per.qiao.A, 那么value=a)

   **获取扩展名图如下**：

   <img src="assets/5ce6111ce4b0c5064aa57fde.png"/>

这里value表示@Adaptive注解的值(数组)中最后一个value, 如果该数组为空，则value等于接口名小写

java生成动态类的模板

```java
package <扩展点接口所在包>;

public class <扩展点接口名>$Adpative implements <扩展点接口> {

  public <有@Adaptive注解的接口方法>(<方法参数>) {
      if(是否有URL类型方法参数?) 使用该URL参数
      else if(是否有方法类型上有URL属性) 使用该URL属性
      <else 在加载扩展点生成自适应扩展点类时抛异常，即加载扩展点失败！>

      if(获取的URL == null) {
          throw new IllegalArgumentException("url == null");
      }

       //...获取扩展点名  图解如上

       <接口> extension = (接口) ExtensionLoader.getExtensionLoader(接口).getExtension(扩展点名);
       extension.<有@Adaptive注解的接口方法>(<方法参数>)
  }

  public <无@Adaptive注解的接口方法>(<方法参数>) {
      throw new UnsupportedOperationException("is not adaptive method!");
  }
}
```

小结：

1. 一个扩展文件内只能有一个扩展类被@Adaptive修饰，而且还需要有其他的初@Adaptive和AOP扩展之外的至少一个其他扩展

2. @Adaptive如果方在类上，那么cachedAdaptiveClass就等于该类

   Holder cachedAdaptiveInstance就包装这该类如果标注在方法上，那么该方法必须有参数为ulr或者参数有返回url的方法，并且会生成动态文件









##服务提供者配置

服务提供者暴露服务配置。对应的配置类：`org.apache.dubbo.config.ServiceConfig`

| 属性        | 对应URL参数       | 类型           | 是否必填 | 缺省值                   | 作用     | 描述                                                         | 兼容性         |
| ----------- | ----------------- | -------------- | -------- | ------------------------ | -------- | ------------------------------------------------------------ | -------------- |
| interface   |                   | class          | **必填** |                          | 服务发现 | 服务接口名                                                   | 1.0.0以上版本  |
| ref         |                   | object         | **必填** |                          | 服务发现 | 服务对象实现引用                                             | 1.0.0以上版本  |
| version     | version           | string         | 可选     | 0.0.0                    | 服务发现 | 服务版本，建议使用两位数字版本，如：1.0，通常在接口不兼容时版本号才需要升级 | 1.0.0以上版本  |
| group       | group             | string         | 可选     |                          | 服务发现 | 服务分组，当一个接口有多个实现，可以用分组区分               | 1.0.7以上版本  |
| path        | <path>            | string         | 可选     | 缺省为接口名             | 服务发现 | 服务路径 (注意：1.0不支持自定义路径，总是使用接口名，如果有1.0调2.0，配置服务路径可能不兼容) | 1.0.12以上版本 |
| delay       | delay             | int            | 可选     | 0                        | 性能调优 | 延迟注册服务时间(毫秒) ，设为-1时，表示延迟到Spring容器初始化完成时暴露服务 | 1.0.14以上版本 |
| timeout     | timeout           | int            | 可选     | 1000                     | 性能调优 | 远程服务调用超时时间(毫秒)                                   | 2.0.0以上版本  |
| retries     | retries           | int            | 可选     | 2                        | 性能调优 | 远程服务调用重试次数，不包括第一次调用，不需要重试请设为0    | 2.0.0以上版本  |
| connections | connections       | int            | 可选     | 100                      | 性能调优 | 对每个提供者的最大连接数，rmi、http、hessian等短连接协议表示限制连接数，dubbo等长连接协表示建立的长连接个数 | 2.0.0以上版本  |
| loadbalance | loadbalance       | string         | 可选     | random                   | 性能调优 | 负载均衡策略，可选值：random,roundrobin,leastactive，分别表示：随机，轮询，最少活跃调用 | 2.0.0以上版本  |
| async       | async             | boolean        | 可选     | false                    | 性能调优 | 是否缺省异步执行，不可靠异步，只是忽略返回值，不阻塞执行线程 | 2.0.0以上版本  |
| local       | local             | class/boolean  | 可选     | false                    | 服务治理 | 设为true，表示使用缺省代理类名，即：接口名 + Local后缀，已废弃，请使用stub | 2.0.0以上版本  |
| stub        | stub              | class/boolean  | 可选     | false                    | 服务治理 | 设为true，表示使用缺省代理类名，即：接口名 + Stub后缀，服务接口客户端本地代理类名，用于在客户端执行本地逻辑，如本地缓存等，该本地代理类的构造函数必须允许传入远程代理对象，构造函数如：public XxxServiceStub(XxxService xxxService) | 2.0.0以上版本  |
| mock        | mock              | class/boolean  | 可选     | false                    | 服务治理 | 设为true，表示使用缺省Mock类名，即：接口名 + Mock后缀，服务接口调用失败Mock实现类，该Mock类必须有一个无参构造函数，与Local的区别在于，Local总是被执行，而Mock只在出现非业务异常(比如超时，网络异常等)时执行，Local在远程调用之前执行，Mock在远程调用后执行。 | 2.0.0以上版本  |
| token       | token             | string/boolean | 可选     | false                    | 服务治理 | 令牌验证，为空表示不开启，如果为true，表示随机生成动态令牌，否则使用静态令牌，令牌的作用是防止消费者绕过注册中心直接访问，保证注册中心的授权功能有效，如果使用点对点调用，需关闭令牌功能 | 2.0.0以上版本  |
| registry    |                   | string         | 可选     | 缺省向所有registry注册   | 配置关联 | 向指定注册中心注册，在多个注册中心时使用，值为<dubbo:registry>的id属性，多个注册中心ID用逗号分隔，如果不想将该服务注册到任何registry，可将值设为N/A | 2.0.0以上版本  |
| provider    |                   | string         | 可选     | 缺使用第一个provider配置 | 配置关联 | 指定provider，值为<dubbo:provider>的id属性                   | 2.0.0以上版本  |
| deprecated  | deprecated        | boolean        | 可选     | false                    | 服务治理 | 服务是否过时，如果设为true，消费方引用时将打印服务过时警告error日志 | 2.0.5以上版本  |
| dynamic     | dynamic           | boolean        | 可选     | true                     | 服务治理 | 服务是否动态注册，如果设为false，注册后将显示后disable状态，需人工启用，并且服务提供者停止时，也不会自动取消册，需人工禁用。 | 2.0.5以上版本  |
| accesslog   | accesslog         | string/boolean | 可选     | false                    | 服务治理 | 设为true，将向logger中输出访问日志，也可填写访问日志文件路径，直接把访问日志输出到指定文件 | 2.0.5以上版本  |
| owner       | owner             | string         | 可选     |                          | 服务治理 | 服务负责人，用于服务治理，请填写负责人公司邮箱前缀           | 2.0.5以上版本  |
| document    | document          | string         | 可选     |                          | 服务治理 | 服务文档URL                                                  | 2.0.5以上版本  |
| weight      | weight            | int            | 可选     |                          | 性能调优 | 服务权重                                                     | 2.0.5以上版本  |
| executes    | executes          | int            | 可选     | 0                        | 性能调优 | 服务提供者每服务每方法最大可并行执行请求数                   | 2.0.5以上版本  |
| proxy       | proxy             | string         | 可选     | javassist                | 性能调优 | 生成动态代理方式，可选：jdk/javassist                        | 2.0.5以上版本  |
| cluster     | cluster           | string         | 可选     | failover                 | 性能调优 | 集群方式，可选：failover/failfast/failsafe/failback/forking  | 2.0.5以上版本  |
| filter      | service.filter    | string         | 可选     | default                  | 性能调优 | 服务提供方远程调用过程拦截器名称，多个名称用逗号分隔         | 2.0.5以上版本  |
| listener    | exporter.listener | string         | 可选     | default                  | 性能调优 | 服务提供方导出服务监听器名称，多个名称用逗号分隔             |                |
| protocol    |                   | string         | 可选     |                          | 配置关联 | 使用指定的协议暴露服务，在多协议时使用，值为<dubbo:protocol>的id属性，多个协议ID用逗号分隔 | 2.0.5以上版本  |
| layer       | layer             | string         | 可选     |                          | 服务治理 | 服务提供者所在的分层。如：biz、dao、intl:web、china:acton。  | 2.0.7以上版本  |
| register    | register          | boolean        | 可选     | true                     | 服务治理 | 该协议的服务是否注册到注册中心                               | 2.0.8以上版本  |





















