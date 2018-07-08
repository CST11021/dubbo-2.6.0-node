package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.UserInfo;

/**
 * @author wb-whz291815
 * @version $Id: HelloService.java, v 0.1 2018-04-03 9:52 wb-whz291815 Exp $$
 */
public class HelloServiceImpl implements com.alibaba.dubbo.demo.HelloService {
    @Override
    public String sayHello(String name) {
        System.out.println("HelloService#sayHello 服务被调用");
        return "Hello " + name;
    }

    @Override
    public UserInfo sayHello(UserInfo userInfo) {
        System.out.println("HelloService#sayHello 服务被调用");
        userInfo.setUsername("Hello " + userInfo.getUsername());
        return userInfo;
    }
}
