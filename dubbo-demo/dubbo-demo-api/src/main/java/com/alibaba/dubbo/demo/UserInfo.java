package com.alibaba.dubbo.demo;

public class UserInfo extends User {
    private String username;

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }
}