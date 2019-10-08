package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.DemoService;

/**
 * @Author: wanghz
 * @Date: 2019/9/28 9:36 AM
 */
public class DemoServiceStub implements DemoService {

    private DemoService demoService;

    public DemoServiceStub(DemoService demoService ) {
        this.demoService = demoService  ;
    }

    @Override
    public String sayHello(String name) {
        if ("李四".equals(name)) {
            return "Hi";
        }
        return demoService.sayHello(name);
    }
}
