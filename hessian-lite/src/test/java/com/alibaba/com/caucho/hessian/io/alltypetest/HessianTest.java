package com.alibaba.com.caucho.hessian.io.alltypetest;

import com.alibaba.com.caucho.hessian.io.HessianOutput;
import com.alibaba.com.caucho.hessian.io.bugtest.UserInfo;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @Author: wanghz
 * @Date: 2020/2/6 9:13 PM
 */
public class HessianTest {

    @Test
    public void test() throws IOException {
        // 1、创建一个要序列化的实例
        AllTypeObject obj = new AllTypeObject();

        // 2、创建一个Hessian的序列化输出流实例，该实例为AbstractHessianOutput的实例
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        HessianOutput ho = new HessianOutput(os);

        // 3、进行序列化
        ho.writeObject(obj);

        byte[] userByte = os.toByteArray();
        for (int i = 0; i < userByte.length; i++) {
            System.out.print((char)userByte[i]);
        }


        System.out.println("\n结束");
    }

}
