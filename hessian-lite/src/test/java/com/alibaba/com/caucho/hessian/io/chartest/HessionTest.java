package com.alibaba.com.caucho.hessian.io.chartest;

import com.alibaba.com.caucho.hessian.io.HessianOutput;
import com.alibaba.com.caucho.hessian.io.bugtest.UserInfo;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author whz
 * @version : HessionTest.java, v 0.1 2018-05-18 14:22 whz Exp $$
 */
public class HessionTest {

    @Test
    public void test() throws IOException {
        // 1、创建一个要序列化的实例
        CharObject charObject = new CharObject();
        charObject.setValue('A');

        // 2、创建一个Hessian的序列化输出流实例，该实例为AbstractHessianOutput的实例
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        HessianOutput ho = new HessianOutput(os);

        // 3、进行序列化
        ho.writeObject(charObject);

        byte[] userByte = os.toByteArray();
        for (int i = 0; i < userByte.length; i++) {
            System.out.print((char)userByte[i]);
        }


        System.out.println("\n结束");



        // byte[] userByte = os.toByteArray();
        // ByteArrayInputStream is = new ByteArrayInputStream(userByte);
        //
        // //Hessian的反序列化读取对象
        // HessianInput hi = new HessianInput(is);
        // UserInfo u = (UserInfo) hi.readObject();
        // System.out.println("姓名：" + u.getUsername());// null
        // System.out.println("年龄：" + u.getAge());     // 21
    }

}