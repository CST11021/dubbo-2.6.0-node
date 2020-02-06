package com.alibaba.com.caucho.hessian.io.chartest;

import com.alibaba.com.caucho.hessian.io.bugtest.UserInfo;

import java.io.Serializable;

/**
 * @Author: wanghz
 * @Date: 2020/2/6 12:00 PM
 */
public class CharObject implements Serializable {

    private char value;

    public char getValue() {
        return value;
    }

    public void setValue(char value) {
        this.value = value;
    }
}
