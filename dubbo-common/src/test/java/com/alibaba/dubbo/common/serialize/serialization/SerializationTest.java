package com.alibaba.dubbo.common.serialize.serialization;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.model.Person;
import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.serialize.Serialization;
import com.alibaba.dubbo.common.serialize.support.dubbo.DubboSerialization;
import com.alibaba.dubbo.common.serialize.support.fst.FstSerialization;
import com.alibaba.dubbo.common.serialize.support.hessian.Hessian2Serialization;
import com.alibaba.dubbo.common.serialize.support.java.CompactedJavaSerialization;
import com.alibaba.dubbo.common.serialize.support.java.JavaSerialization;
import com.alibaba.dubbo.common.serialize.support.json.FastJsonSerialization;
import com.alibaba.dubbo.common.serialize.support.json.JsonSerialization;
import com.alibaba.dubbo.common.serialize.support.kryo.KryoSerialization;
import com.alibaba.dubbo.common.serialize.support.nativejava.NativeJavaSerialization;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @author whz
 * @version : SerializationTest.java, v 0.1 2018-05-18 17:11 whz Exp $$
 */
public class SerializationTest {

    @Test
    public void DubboSerializationTest() throws IOException, ClassNotFoundException {
        Serialization serialization = new DubboSerialization();
        Person person = (Person) doSeriazation(serialization);
        System.out.println(person);
    }

    @Test
    public void Hessian2SerializationTest() throws IOException, ClassNotFoundException {
        Serialization serialization = new Hessian2Serialization();
        Person person = (Person) doSeriazation(serialization);
        System.out.println(person);
    }

    @Test
    public void JavaSerializationTest() throws IOException, ClassNotFoundException {
        Serialization serialization = new JavaSerialization();
        Person person = (Person) doSeriazation(serialization);
        System.out.println(person);
    }

    @Test
    public void CompactedJavaSerializationTest() throws IOException, ClassNotFoundException {
        Serialization serialization = new CompactedJavaSerialization();
        Person person = (Person) doSeriazation(serialization);
        System.out.println(person);
    }

    @Test
    public void JsonSerializationTest() throws IOException, ClassNotFoundException {
        Serialization serialization = new JsonSerialization();
        Map person = (Map) doSeriazation(serialization);
        System.out.println(person);
    }

    @Test
    public void FastJsonSerializationTest() throws IOException, ClassNotFoundException {
        Serialization serialization = new FastJsonSerialization();
        JSONObject jsonObject = (JSONObject) doSeriazation(serialization);
        Person person = JSONObject.parseObject(jsonObject.toJSONString(), Person.class);
        System.out.println(person);
    }

    @Test
    public void NativeJavaSerializationTest() throws IOException, ClassNotFoundException {
        Serialization serialization = new NativeJavaSerialization();
        Person person = (Person) doSeriazation(serialization);
        System.out.println(person);
    }

    @Test
    public void KryoSerializationTest() throws IOException, ClassNotFoundException {
        Serialization serialization = new KryoSerialization();
        Person person = (Person) doSeriazation(serialization);
        System.out.println(person);
    }

    @Test
    public void FstSerializationTest() throws IOException, ClassNotFoundException {
        Serialization serialization = new FstSerialization();
        Person person = (Person) doSeriazation(serialization);
        System.out.println(person);
    }


    private Object doSeriazation(Serialization serialization) throws IOException, ClassNotFoundException {
        URL url = new URL("protocl", "1.1.1.1", 1234);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeObject(new Person());
        objectOutput.flushBuffer();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        ObjectInput deserialize = serialization.deserialize(url, byteArrayInputStream);

        return deserialize.readObject();
    }
}
