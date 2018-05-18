package com.alibaba.com.caucho.hessian.io.bugtest;

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