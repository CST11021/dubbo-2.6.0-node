/*
 * Copyright (c) 2001-2008 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Burlap", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */

package com.alibaba.com.caucho.hessian.io;

import com.alibaba.com.caucho.hessian.io.java8.DurationHandle;
import com.alibaba.com.caucho.hessian.io.java8.InstantHandle;
import com.alibaba.com.caucho.hessian.io.java8.LocalDateHandle;
import com.alibaba.com.caucho.hessian.io.java8.LocalDateTimeHandle;
import com.alibaba.com.caucho.hessian.io.java8.LocalTimeHandle;
import com.alibaba.com.caucho.hessian.io.java8.MonthDayHandle;
import com.alibaba.com.caucho.hessian.io.java8.OffsetDateTimeHandle;
import com.alibaba.com.caucho.hessian.io.java8.OffsetTimeHandle;
import com.alibaba.com.caucho.hessian.io.java8.PeriodHandle;
import com.alibaba.com.caucho.hessian.io.java8.YearHandle;
import com.alibaba.com.caucho.hessian.io.java8.YearMonthHandle;
import com.alibaba.com.caucho.hessian.io.java8.ZoneIdSerializer;
import com.alibaba.com.caucho.hessian.io.java8.ZoneOffsetHandle;
import com.alibaba.com.caucho.hessian.io.java8.ZonedDateTimeHandle;

import javax.management.ObjectName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.alibaba.com.caucho.hessian.io.java8.Java8TimeSerializer.create;

/**
 * 返回序列化方法的工厂：
 * SerializerFactory继承AbstractSerializerFactory，而且在SerializerFactory有很多静态map用来存放类与序列化和反序列化工具类的映射，
 * 这样如果已经用过的序列化工具就可以直接拿出来用，不必再重新实例化工具类。
 *
 * 在SerializerFactory中，实现了抽象类的getSerializer方法，根据不同的需要被序列化的类来获得不同的序列化工具，一共有17种序列化工具，
 * hessian为不同的类型的java对象实现了不同的序列化工具，默认的序列化工具是JavaSerializer
 */
public class SerializerFactory extends AbstractSerializerFactory {

    private static final Logger log = Logger.getLogger(SerializerFactory.class.getName());

    private static Deserializer OBJECT_DESERIALIZER = new BasicDeserializer(BasicDeserializer.OBJECT);

    /** 缓存Class对应的Serializer实例 */
    private static HashMap _staticSerializerMap;
    /** 缓存Class对应的Deserializer实例 */
    private static HashMap _staticDeserializerMap;
    /** 缓存Class类型名对应的Deserializer实例 */
    private static HashMap _staticTypeMap;

    // 初始化：_staticSerializerMap、_staticDeserializerMap和_staticTypeMap
    static {
        _staticSerializerMap = new HashMap();
        _staticDeserializerMap = new HashMap();
        _staticTypeMap = new HashMap();

        // void类型

        addBasic(void.class, "void", BasicSerializer.NULL);

        // 基础包装类型

        addBasic(Boolean.class, "boolean", BasicSerializer.BOOLEAN);
        addBasic(Byte.class, "byte", BasicSerializer.BYTE);
        addBasic(Short.class, "short", BasicSerializer.SHORT);
        addBasic(Integer.class, "int", BasicSerializer.INTEGER);
        addBasic(Long.class, "long", BasicSerializer.LONG);
        addBasic(Float.class, "float", BasicSerializer.FLOAT);
        addBasic(Double.class, "double", BasicSerializer.DOUBLE);
        addBasic(Character.class, "char", BasicSerializer.CHARACTER_OBJECT);
        addBasic(String.class, "string", BasicSerializer.STRING);
        addBasic(Object.class, "object", BasicSerializer.OBJECT);
        addBasic(java.util.Date.class, "date", BasicSerializer.DATE);

        // 基础类型

        addBasic(boolean.class, "boolean", BasicSerializer.BOOLEAN);
        addBasic(byte.class, "byte", BasicSerializer.BYTE);
        addBasic(short.class, "short", BasicSerializer.SHORT);
        addBasic(int.class, "int", BasicSerializer.INTEGER);
        addBasic(long.class, "long", BasicSerializer.LONG);
        addBasic(float.class, "float", BasicSerializer.FLOAT);
        addBasic(double.class, "double", BasicSerializer.DOUBLE);
        addBasic(char.class, "char", BasicSerializer.CHARACTER);

        // 数组类型

        addBasic(boolean[].class, "[boolean", BasicSerializer.BOOLEAN_ARRAY);
        addBasic(byte[].class, "[byte", BasicSerializer.BYTE_ARRAY);
        addBasic(short[].class, "[short", BasicSerializer.SHORT_ARRAY);
        addBasic(int[].class, "[int", BasicSerializer.INTEGER_ARRAY);
        addBasic(long[].class, "[long", BasicSerializer.LONG_ARRAY);
        addBasic(float[].class, "[float", BasicSerializer.FLOAT_ARRAY);
        addBasic(double[].class, "[double", BasicSerializer.DOUBLE_ARRAY);
        addBasic(char[].class, "[char", BasicSerializer.CHARACTER_ARRAY);
        addBasic(String[].class, "[string", BasicSerializer.STRING_ARRAY);
        addBasic(Object[].class, "[object", BasicSerializer.OBJECT_ARRAY);

        _staticSerializerMap.put(Class.class, new ClassSerializer());
        _staticDeserializerMap.put(Number.class, new BasicDeserializer(BasicSerializer.NUMBER));
        _staticSerializerMap.put(BigDecimal.class, new StringValueSerializer());

        try {
            _staticDeserializerMap.put(BigDecimal.class, new StringValueDeserializer(BigDecimal.class));
            _staticDeserializerMap.put(BigInteger.class, new BigIntegerDeserializer());
        } catch (Throwable e) {
        }

        _staticSerializerMap.put(File.class, new StringValueSerializer());
        try {
            _staticDeserializerMap.put(File.class, new StringValueDeserializer(File.class));
        } catch (Throwable e) {
        }

        _staticSerializerMap.put(ObjectName.class, new StringValueSerializer());
        try {
            _staticDeserializerMap.put(ObjectName.class, new StringValueDeserializer(ObjectName.class));
        } catch (Throwable e) {
        }

        _staticSerializerMap.put(java.sql.Date.class, new SqlDateSerializer());
        _staticSerializerMap.put(java.sql.Time.class, new SqlDateSerializer());
        _staticSerializerMap.put(java.sql.Timestamp.class, new SqlDateSerializer());

        _staticSerializerMap.put(java.io.InputStream.class, new InputStreamSerializer());
        _staticDeserializerMap.put(java.io.InputStream.class, new InputStreamDeserializer());

        try {
            _staticDeserializerMap.put(java.sql.Date.class, new SqlDateDeserializer(java.sql.Date.class));
            _staticDeserializerMap.put(java.sql.Time.class, new SqlDateDeserializer(java.sql.Time.class));
            _staticDeserializerMap.put(java.sql.Timestamp.class, new SqlDateDeserializer(java.sql.Timestamp.class));
        } catch (Throwable e) {
            e.printStackTrace();
        }

        // hessian/3bb5
        try {
            Class stackTrace = StackTraceElement.class;

            _staticDeserializerMap.put(stackTrace, new StackTraceElementDeserializer());
        } catch (Throwable e) {
        }

        // 添加java8中时间序列化处理类
        try {
            if (isJava8()) {
                _staticSerializerMap.put(Class.forName("java.time.LocalTime"), create(LocalTimeHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.LocalDate"), create(LocalDateHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.LocalDateTime"), create(LocalDateTimeHandle.class));

                _staticSerializerMap.put(Class.forName("java.time.Instant"), create(InstantHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.Duration"), create(DurationHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.Period"), create(PeriodHandle.class));

                _staticSerializerMap.put(Class.forName("java.time.Year"), create(YearHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.YearMonth"), create(YearMonthHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.MonthDay"), create(MonthDayHandle.class));

                _staticSerializerMap.put(Class.forName("java.time.OffsetDateTime"), create(OffsetDateTimeHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.ZoneOffset"), create(ZoneOffsetHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.OffsetTime"), create(OffsetTimeHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.ZonedDateTime"), create(ZonedDateTimeHandle.class));
            }
        } catch (Throwable t) {
            log.warning(String.valueOf(t.getCause()));
        }
    }

    /** 默认的Serializer，默认为JavaSerializer，一些自定义的类类型，比如User等都会返回默认的 */
    protected Serializer _defaultSerializer;

    /** 额外的AbstractSerializerFactory工厂，一般为空 */
    protected ArrayList _factories = new ArrayList();

    /** 处理集合类型的序列化 */
    protected CollectionSerializer _collectionSerializer;
    /** 处理Map类型的序列化 */
    protected MapSerializer _mapSerializer;

    /** 有时序列化，需要实例化一个类 */
    private ClassLoader _loader;
    private Deserializer _hashMapDeserializer;
    private Deserializer _arrayListDeserializer;

    /** 每次获取类对应的Serializer后，都会缓存起来 */
    private HashMap _cachedSerializerMap;
    /** 每次获取类对应的Deserializer后，都会缓存起来 */
    private HashMap _cachedDeserializerMap;
    /** 每次获取类对应的Deserializer后，都会缓存起来 */
    private HashMap _cachedTypeDeserializerMap;

    /** 表示是否允许序列化的类型不继承Serializable */
    private boolean _isAllowNonSerializable;



    // 构造器

    public SerializerFactory() {
        this(Thread.currentThread().getContextClassLoader());
    }
    public SerializerFactory(ClassLoader loader) {
        _loader = loader;
    }



    /**
     * 根据类来决定用哪种序列化工具类。
     * 根据class返回一个Serializer实例，实现了Serializer接口的类才可以被序列化（_isAllowNonSerializable可以控制，一般情况下需要实现该接口）
     *
     * @param cl 需要序列化的对象的类
     * @return
     */
    public Serializer getSerializer(Class cl) throws HessianProtocolException {
        Serializer serializer;

        // 1、先从缓存获取对应的Serializer实例，如果是基础类型的一般可以从该缓存中获取到，如果是自定义的类类型比如User，则返回为null
        serializer = (Serializer) _staticSerializerMap.get(cl);
        if (serializer != null)
            return serializer;

        // 2、从_cachedSerializerMap获取，看看这个类之前是否序列化过
        if (_cachedSerializerMap != null) {
            synchronized (_cachedSerializerMap) {
                serializer = (Serializer) _cachedSerializerMap.get(cl);
            }

            if (serializer != null)
                return serializer;
        }

        // 遍历工厂，看看能不能获取到对应的Serializer，一般扩展工厂都为空
        for (int i = 0; serializer == null && _factories != null && i < _factories.size(); i++) {
            AbstractSerializerFactory factory;
            factory = (AbstractSerializerFactory) _factories.get(i);
            serializer = factory.getSerializer(cl);
        }



        if (serializer != null) {

        }
        //must before "else if (JavaSerializer.getWriteReplace(cl) != null)"
        else if (isZoneId(cl))
            serializer = ZoneIdSerializer.getInstance();
        else if (isEnumSet(cl))
            serializer = EnumSetSerializer.getInstance();
        else if (JavaSerializer.getWriteReplace(cl) != null)
            serializer = new JavaSerializer(cl, _loader);
        else if (HessianRemoteObject.class.isAssignableFrom(cl))
            serializer = new RemoteSerializer();
        else if (Map.class.isAssignableFrom(cl)) {
            if (_mapSerializer == null)
                _mapSerializer = new MapSerializer();
            serializer = _mapSerializer;
        } else if (Collection.class.isAssignableFrom(cl)) {
            if (_collectionSerializer == null) {
                _collectionSerializer = new CollectionSerializer();
            }
            serializer = _collectionSerializer;
        } else if (cl.isArray())
            serializer = new ArraySerializer();
        else if (Throwable.class.isAssignableFrom(cl))
            serializer = new ThrowableSerializer(cl, getClassLoader());
        else if (InputStream.class.isAssignableFrom(cl))
            serializer = new InputStreamSerializer();
        else if (Iterator.class.isAssignableFrom(cl))
            serializer = IteratorSerializer.create();
        else if (Enumeration.class.isAssignableFrom(cl))
            serializer = EnumerationSerializer.create();
        else if (Calendar.class.isAssignableFrom(cl))
            serializer = CalendarSerializer.create();
        else if (Locale.class.isAssignableFrom(cl))
            serializer = LocaleSerializer.create();
        else if (Enum.class.isAssignableFrom(cl))
            serializer = new EnumSerializer(cl);





        // 上面的都获取不到serializer，则使用默认的序列化工具
        if (serializer == null)
            serializer = getDefaultSerializer(cl);

        // 返回前添加到缓存
        if (_cachedSerializerMap == null)
            _cachedSerializerMap = new HashMap(8);
        synchronized (_cachedSerializerMap) {
            _cachedSerializerMap.put(cl, serializer);
        }

        return serializer;
    }

    /**
     * 根据类来决定用哪种反序列化工具类。
     * 根据class返回一个Serializer实例，实现了Serializer接口的类才可以被反序列化（_isAllowNonSerializable可以控制，一般情况下需要实现该接口）
     *
     * @param cl 需要反序列化的对象的类
     * @return
     */
    public Deserializer getDeserializer(Class cl) throws HessianProtocolException {
        Deserializer deserializer;

        // 1、先从静态缓存里获取
        deserializer = (Deserializer) _staticDeserializerMap.get(cl);
        if (deserializer != null)
            return deserializer;

        if (_cachedDeserializerMap != null) {
            synchronized (_cachedDeserializerMap) {
                deserializer = (Deserializer) _cachedDeserializerMap.get(cl);
            }
            if (deserializer != null)
                return deserializer;
        }

        // 一般扩展工厂都为空
        for (int i = 0; deserializer == null && _factories != null && i < _factories.size(); i++) {
            AbstractSerializerFactory factory;
            factory = (AbstractSerializerFactory) _factories.get(i);
            deserializer = factory.getDeserializer(cl);
        }


        if (deserializer != null) {
        } else if (Collection.class.isAssignableFrom(cl))
            deserializer = new CollectionDeserializer(cl);
        else if (Map.class.isAssignableFrom(cl))
            deserializer = new MapDeserializer(cl);
        else if (cl.isInterface())
            deserializer = new ObjectDeserializer(cl);
        else if (cl.isArray())
            deserializer = new ArrayDeserializer(cl.getComponentType());
        else if (Enumeration.class.isAssignableFrom(cl))
            deserializer = EnumerationDeserializer.create();
        else if (Enum.class.isAssignableFrom(cl))
            deserializer = new EnumDeserializer(cl);
        else if (Class.class.equals(cl))
            deserializer = new ClassDeserializer(_loader);
        else
            deserializer = getDefaultDeserializer(cl);


        // 确定了Deserializer后，缓存起来
        if (_cachedDeserializerMap == null)
            _cachedDeserializerMap = new HashMap(8);
        synchronized (_cachedDeserializerMap) {
            _cachedDeserializerMap.put(cl, deserializer);
        }

        return deserializer;
    }





    /**
     * 添加基础类型对应序列化实例和反序列化工具类的实例
     *
     * @param cl            基础类型
     * @param typeName      类型名称
     * @param type          类型值，每个基础类型都有对应的值
     */
    private static void addBasic(Class cl, String typeName, int type) {
        // 1、添加类型对应的Serializer实例
        _staticSerializerMap.put(cl, new BasicSerializer(type));

        // 2、添加类型对应的Deserializer实例
        Deserializer deserializer = new BasicDeserializer(type);
        _staticDeserializerMap.put(cl, deserializer);

        // 3、添加类型名对应的Deserializer实例
        _staticTypeMap.put(typeName, deserializer);
    }

    /**
     * Set true if the collection serializer should send the java type.
     */
    public void setSendCollectionType(boolean isSendType) {
        if (_collectionSerializer == null)
            _collectionSerializer = new CollectionSerializer();

        _collectionSerializer.setSendJavaType(isSendType);

        if (_mapSerializer == null)
            _mapSerializer = new MapSerializer();

        _mapSerializer.setSendJavaType(isSendType);
    }

    /**
     * Adds a factory.
     */
    public void addFactory(AbstractSerializerFactory factory) {
        _factories.add(factory);
    }

    /**
     * If true, non-serializable objects are allowed.
     */
    public boolean isAllowNonSerializable() {
        return _isAllowNonSerializable;
    }

    /**
     * If true, non-serializable objects are allowed.
     */
    public void setAllowNonSerializable(boolean allow) {
        _isAllowNonSerializable = allow;
    }

    /**
     * 返回默认的Serializer，默认为JavaSerializer
     *
     * @param cl
     * @return
     */
    protected Serializer getDefaultSerializer(Class cl) {
        if (_defaultSerializer != null)
            return _defaultSerializer;

        // 序列化类一定要继承Serializable
        if (!Serializable.class.isAssignableFrom(cl) && !_isAllowNonSerializable) {
            throw new IllegalStateException("Serialized class " + cl.getName() + " must implement java.io.Serializable");
        }

        return new JavaSerializer(cl, _loader);
    }

    /**
     * Returns the default serializer for a class that isn't matched
     * directly.  Application can override this method to produce
     * bean-style serialization instead of field serialization.
     *
     * @param cl the class of the object that needs to be serialized.
     * @return a serializer object for the serialization.
     */
    protected Deserializer getDefaultDeserializer(Class cl) {
        return new JavaDeserializer(cl);
    }

    /**
     * Reads the object as a list.
     */
    public Object readList(AbstractHessianInput in, int length, String type) throws HessianProtocolException, IOException {
        Deserializer deserializer = getDeserializer(type);

        if (deserializer != null)
            return deserializer.readList(in, length);
        else
            return new CollectionDeserializer(ArrayList.class).readList(in, length);
    }

    /**
     * Reads the object as a map.
     */
    public Object readMap(AbstractHessianInput in, String type) throws HessianProtocolException, IOException {
        Deserializer deserializer = getDeserializer(type);

        if (deserializer != null)
            return deserializer.readMap(in);
        else if (_hashMapDeserializer != null)
            return _hashMapDeserializer.readMap(in);
        else {
            _hashMapDeserializer = new MapDeserializer(HashMap.class);

            return _hashMapDeserializer.readMap(in);
        }
    }

    /**
     * Reads the object as a map.
     */
    public Object readObject(AbstractHessianInput in, String type, String[] fieldNames) throws HessianProtocolException, IOException {
        Deserializer deserializer = getDeserializer(type);

        if (deserializer != null)
            return deserializer.readObject(in, fieldNames);
        else if (_hashMapDeserializer != null)
            return _hashMapDeserializer.readObject(in, fieldNames);
        else {
            _hashMapDeserializer = new MapDeserializer(HashMap.class);

            return _hashMapDeserializer.readObject(in, fieldNames);
        }
    }

    /**
     * Reads the object as a map.
     */
    public Deserializer getObjectDeserializer(String type, Class cl) throws HessianProtocolException {
        Deserializer reader = getObjectDeserializer(type);

        if (cl == null
                || cl.equals(reader.getType())
                || cl.isAssignableFrom(reader.getType())
                || HessianHandle.class.isAssignableFrom(reader.getType())) {
            return reader;
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine("hessian: expected '" + cl.getName() + "' at '" + type + "' ("
                    + reader.getType().getName() + ")");
        }

        return getDeserializer(cl);
    }

    /**
     * Reads the object as a map.
     */
    public Deserializer getObjectDeserializer(String type) throws HessianProtocolException {
        Deserializer deserializer = getDeserializer(type);

        if (deserializer != null)
            return deserializer;
        else if (_hashMapDeserializer != null)
            return _hashMapDeserializer;
        else {
            _hashMapDeserializer = new MapDeserializer(HashMap.class);

            return _hashMapDeserializer;
        }
    }

    /**
     * Reads the object as a map.
     */
    public Deserializer getListDeserializer(String type, Class cl) throws HessianProtocolException {
        Deserializer reader = getListDeserializer(type);

        if (cl == null
                || cl.equals(reader.getType())
                || cl.isAssignableFrom(reader.getType())) {
            return reader;
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine("hessian: expected '" + cl.getName() + "' at '" + type + "' ("
                    + reader.getType().getName() + ")");
        }

        return getDeserializer(cl);
    }

    /**
     * Reads the object as a map.
     */
    public Deserializer getListDeserializer(String type) throws HessianProtocolException {
        Deserializer deserializer = getDeserializer(type);

        if (deserializer != null)
            return deserializer;
        else if (_arrayListDeserializer != null)
            return _arrayListDeserializer;
        else {
            _arrayListDeserializer = new CollectionDeserializer(ArrayList.class);

            return _arrayListDeserializer;
        }
    }

    /**
     * Returns a deserializer based on a string type.
     */
    public Deserializer getDeserializer(String type) throws HessianProtocolException {
        if (type == null || type.equals(""))
            return null;

        Deserializer deserializer;

        if (_cachedTypeDeserializerMap != null) {
            synchronized (_cachedTypeDeserializerMap) {
                deserializer = (Deserializer) _cachedTypeDeserializerMap.get(type);
            }

            if (deserializer != null)
                return deserializer;
        }


        deserializer = (Deserializer) _staticTypeMap.get(type);
        if (deserializer != null)
            return deserializer;

        if (type.startsWith("[")) {
            Deserializer subDeserializer = getDeserializer(type.substring(1));

            if (subDeserializer != null)
                deserializer = new ArrayDeserializer(subDeserializer.getType());
            else
                deserializer = new ArrayDeserializer(Object.class);
        } else {
            try {
                Class cl = Class.forName(type, false, _loader);
                deserializer = getDeserializer(cl);
            } catch (Exception e) {
                log.warning("Hessian/Burlap: '" + type + "' is an unknown class in " + _loader + ":\n" + e);

                log.log(Level.FINER, e.toString(), e);
            }
        }

        if (deserializer != null) {
            if (_cachedTypeDeserializerMap == null)
                _cachedTypeDeserializerMap = new HashMap(8);

            synchronized (_cachedTypeDeserializerMap) {
                _cachedTypeDeserializerMap.put(type, deserializer);
            }
        }

        return deserializer;
    }

    private static boolean isZoneId(Class cl) {
        try {
            return isJava8() && Class.forName("java.time.ZoneId").isAssignableFrom(cl);
        } catch (ClassNotFoundException e) {
            // ignore
        }
        return false;
    }

    private static boolean isEnumSet(Class cl) {
        return EnumSet.class.isAssignableFrom(cl);
    }

    /**
     * 检查是否为java8环境
     *
     * @return if on java 8
     */
    private static boolean isJava8() {
        String javaVersion = System.getProperty("java.specification.version");
        return Double.valueOf(javaVersion) >= 1.8;
    }

    public ClassLoader getClassLoader() {
        return _loader;
    }
}
