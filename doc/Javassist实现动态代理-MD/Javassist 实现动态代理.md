# Javassist 实现动态代理



Javassist 生成动态代理可以使用两种方式，一种使用代理工厂创建，和普通的JDK动态代理和 CGLIB类似，另一种则可以使用 动态代码创建。

使用 Javassist 需要添加下面依赖。

```xml
<dependency>
    <groupId>org.javassist</groupId>
    <artifactId>javassist</artifactId>
    <version>3.22.0-GA</version>
    <scope>compile</scope>
    <optional>true</optional>
</dependency>
```

下面我们看具体的代码

```tsx

import javassist.*;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import java.lang.reflect.Method;

/**
 * javassist 生成动态代理可以使用两种方式，一种使用代理工厂创建，和普通的JDK动态代理和 CGLIB
 * 类似，另一种则可以使用 动态代码创建
 */
public class TestMain {
    public static void main(String[] args) throws Exception {
        testJavassistFactoryProxy();
        testJavassistDefineClass();
    }

    // 代理工厂创建动态代理
    public  static  void testJavassistFactoryProxy() throws Exception {
        // 创建代理工厂
        ProxyFactory proxyFactory = new ProxyFactory();

        // 设置被代理类的类型
        proxyFactory.setSuperclass(RayTest.class);

        // 创建代理类的class
        Class<ProxyObject> proxyClass = proxyFactory.createClass();

        // 创建对象
        RayTest proxyTest = (RayTest)proxyClass.newInstance();

        ((ProxyObject) proxyTest).setHandler(new MethodHandler() {
            // 真实主题
            RayTest test = new RayTest();

            public Object invoke(Object self, Method thisMethod,
                                 Method proceed, Object[] args) throws Throwable {
                String before = "before ";
                Object str = thisMethod.invoke(test, args);
                String after = " after";
                return before + str + after;
            }
        });
        String exe = proxyTest.exe();
        System.out.println(exe);

    }

    // 动态代码创建的例子
    // 下面例子使用 Javassist 的 API成功组织出代理类的一个子类，可以看出 添加构造函数，添加属性，
    // 添加方法，内容 都是通过字符串类型完成即可。 通过 Javassist 强大的字节生成能力可以达到动态
    // 增加类和实现动态代理的功能.
    public static void testJavassistDefineClass() throws Exception  {
        // 创建类池，true 表示使用默认路径
        ClassPool classPool = new ClassPool(true);

        String className = RayTest.class.getName();
        // 创建一个类 RayTestJavassistProxy
        CtClass ctClass = classPool.makeClass(className + "JavassistProxy");

        // 添加超类
        // 设置 RayTestJavassistProxy 的父类是 RayTest.
        ctClass.setSuperclass(classPool.get(RayTest.class.getName()));

        // 添加默认构造函数
        ctClass.addConstructor(CtNewConstructor.defaultConstructor(ctClass));

        // 添加属性
        ctClass.addField(CtField.make("public " + className + " real = new " +
                    className + "();", ctClass));

        // 添加方法，里面进行动态代理 logic
        ctClass.addMethod(CtNewMethod.make("public String exe() { return \"before \" + real.exe() + \" after\";}",
                ctClass));
        Class<RayTest> testClass = ctClass.toClass();
        RayTest rayTest = testClass.newInstance();
        String exe = rayTest.exe();
        System.out.println(exe);


    }
}
```



被代理类：

```java
public class RayTest {

    public String exe() {

        return "test";
    }

}
```