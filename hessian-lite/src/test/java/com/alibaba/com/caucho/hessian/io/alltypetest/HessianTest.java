package com.alibaba.com.caucho.hessian.io.alltypetest;

import com.alibaba.com.caucho.hessian.io.HessianOutput;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

/**
 * @Author: wanghz
 * @Date: 2020/2/6 9:13 PM
 */
public class HessianTest {

    @Test
    public void test() throws IOException {
        // 1、创建一个要序列化的实例
        AllTypeObject obj = new AllTypeObject();

        obj.set_String("0");
        obj.set_StringArray(new String[] {"0"});

        obj.set_Class(AllTypeObject.class);
        obj.set_ClassArray(new Class[] {AllTypeObject.class});

        // obj.set_File(new File(""));
        // obj.set_FileArray();

        // obj.set_Number(new Number() {
        //     @Override
        //     public int intValue() {
        //         return 0;
        //     }
        //
        //     @Override
        //     public long longValue() {
        //         return 0;
        //     }
        //
        //     @Override
        //     public float floatValue() {
        //         return 0;
        //     }
        //
        //     @Override
        //     public double doubleValue() {
        //         return 0;
        //     }
        // });
        obj.set_BigDecimal(new BigDecimal(0));
        obj.set_BigInteger(new BigInteger("0"));

        obj.set_boolean(true);
        obj.set_Boolean(Boolean.TRUE);
        obj.set_booleanArray(new boolean[] {Boolean.TRUE});

        obj.set_byte((byte) 0);
        obj.set_Byte(new Byte("0"));
        obj.set_byteArray(new byte[] {(byte) 0});

        obj.set_short((short) 0);
        obj.set_Short(new Short("0"));
        obj.set_shortArray(new short[] {(short) 0});

        obj.set_int(0);
        obj.set_Integer(Integer.valueOf(0));
        obj.set_intArray(new int[0]);

        obj.set_Long(0L);
        obj.set_long(0L);
        obj.set_longArray(new long[0]);

        obj.set_float(0f);
        obj.set_Float(0f);
        obj.set_floatArray(new float[0]);

        obj.set_Double(0D);
        obj.set_double(0D);
        obj.set_doubleArray(new double[0]);

        obj.set_char('0');
        obj.set_Character(new Character('0'));
        obj.set_charArray(new char['0']);

        long curTime = System.currentTimeMillis();
        obj.set_utilDate(new Date(curTime));
        obj.set_sqlDate(new java.sql.Date(curTime));
        obj.set_Time(new Time(curTime));
        obj.set_Timestamp(new Timestamp(curTime));

        // 2、创建一个Hessian的序列化输出流实例，该实例为AbstractHessianOutput的实例
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        HessianOutput ho = new HessianOutput(os);

        // 3、进行序列化
        ho.writeObject(obj);

        System.out.println("\n输出字符：");
        byte[] userByte = os.toByteArray();
        for (int i = 0; i < userByte.length; i++) {
            System.out.print((char)userByte[i]);
        }

        System.out.println("\n输出字节：");
        for (int i = 0; i < userByte.length; i++) {
            System.out.print(userByte[i] + " ");
        }

        System.out.println("\n输出字节和字符：");
        for (int i = 0; i < userByte.length; i++) {
            System.out.print(userByte[i] + "（" + (char)userByte[i] + "）");
        }
        System.out.println("\n结束");
    }

}
