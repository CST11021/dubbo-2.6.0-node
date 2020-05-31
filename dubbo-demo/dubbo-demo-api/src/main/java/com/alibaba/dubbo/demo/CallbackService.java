package com.alibaba.dubbo.demo;

public interface CallbackService {
    void addListener(String key, CallbackListener listener);
}