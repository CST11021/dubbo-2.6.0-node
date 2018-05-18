package com.alibaba.dubbo.demo;

/**
 * @author wb-whz291815
 * @version $Id: HelloService.java, v 0.1 2018-04-03 9:50 wb-whz291815 Exp $$
 */
public interface HelloService {

    String sayHello(String name);

    /**
     * 默认使用Hession序列化方式，这种序列化方法有一个bug：当用hessian序列化对象是一个对象继承另外一个对象的时候，当一个属性在
     * 子类和有一个相同属性的时候，反序列化后子类属性总是为null。所以，这里的返回的UserInfo#userName属性一直为null
     *
     * @param userInfo
     * @return
     */
    UserInfo sayHello(UserInfo userInfo);

}
