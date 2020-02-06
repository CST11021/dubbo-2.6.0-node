package com.alibaba.com.caucho.hessian.io.alltypetest;

import com.alibaba.com.caucho.hessian.io.java8.LocalDateHandle;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;

/**
 * @Author: wanghz
 * @Date: 2020/2/6 8:49 PM
 */
public class AllTypeObject implements Serializable {

    private Void _void;

    private String _String;
    private String[] _StringArray;

    private Object _Object;
    private Object[] _ObjectArray;

    private Class _Class;
    private Class[] _ClassArray;

    private File _File;
    private File[] _FileArray;

    private Number _Number;
    private BigDecimal _BigDecimal;
    private BigInteger _BigInteger;


    private boolean _boolean;
    private Boolean _Boolean;
    private boolean[] _booleanArray;

    private byte _byte;
    private Byte _Byte;
    private byte[] _byteArray;

    private short _short;
    private Short _Short;
    private short[] _shortArray;

    private int _int;
    private Integer _Integer;
    private int[] _intArray;

    private long _long;
    private Long _Long;
    private long[] _longArray;

    private float _float;
    private Float _Float;
    private float _floatArray;

    private double _double;
    private Double _Double;
    private double[] _doubleArray;

    private char _char;
    private Character _Character;
    private char[] _charArray;

    private java.util.Date _utilDate;
    private java.sql.Date _sqlDate;
    private Time _Time;
    private Timestamp _Timestamp;


    // java8

    private LocalTime _LocalTime;
    private LocalDateHandle _LocalDateHandle;
    private LocalDateTime _LocalDateTime;
    private Instant _Instant;
    private Duration _Duration;
    private Period _Period;
    private Year _Year;
    private YearMonth _YearMonth;
    private MonthDay _MonthDay;
    private OffsetDateTime _OffsetDateTime;
    private ZoneOffset _ZoneOffset;
    private OffsetTime _OffsetTime;
    private ZonedDateTime _ZonedDateTime;

    public Void get_void() {
        return _void;
    }

    public void set_void(Void _void) {
        this._void = _void;
    }

    public String get_String() {
        return _String;
    }

    public void set_String(String _String) {
        this._String = _String;
    }

    public String[] get_StringArray() {
        return _StringArray;
    }

    public void set_StringArray(String[] _StringArray) {
        this._StringArray = _StringArray;
    }

    public Object get_Object() {
        return _Object;
    }

    public void set_Object(Object _Object) {
        this._Object = _Object;
    }

    public Object[] get_ObjectArray() {
        return _ObjectArray;
    }

    public void set_ObjectArray(Object[] _ObjectArray) {
        this._ObjectArray = _ObjectArray;
    }

    public Class get_Class() {
        return _Class;
    }

    public void set_Class(Class _Class) {
        this._Class = _Class;
    }

    public Class[] get_ClassArray() {
        return _ClassArray;
    }

    public void set_ClassArray(Class[] _ClassArray) {
        this._ClassArray = _ClassArray;
    }

    public File get_File() {
        return _File;
    }

    public void set_File(File _File) {
        this._File = _File;
    }

    public File[] get_FileArray() {
        return _FileArray;
    }

    public void set_FileArray(File[] _FileArray) {
        this._FileArray = _FileArray;
    }

    public Number get_Number() {
        return _Number;
    }

    public void set_Number(Number _Number) {
        this._Number = _Number;
    }

    public BigDecimal get_BigDecimal() {
        return _BigDecimal;
    }

    public void set_BigDecimal(BigDecimal _BigDecimal) {
        this._BigDecimal = _BigDecimal;
    }

    public BigInteger get_BigInteger() {
        return _BigInteger;
    }

    public void set_BigInteger(BigInteger _BigInteger) {
        this._BigInteger = _BigInteger;
    }

    public boolean is_boolean() {
        return _boolean;
    }

    public void set_boolean(boolean _boolean) {
        this._boolean = _boolean;
    }

    public Boolean get_Boolean() {
        return _Boolean;
    }

    public void set_Boolean(Boolean _Boolean) {
        this._Boolean = _Boolean;
    }

    public boolean[] get_booleanArray() {
        return _booleanArray;
    }

    public void set_booleanArray(boolean[] _booleanArray) {
        this._booleanArray = _booleanArray;
    }

    public byte get_byte() {
        return _byte;
    }

    public void set_byte(byte _byte) {
        this._byte = _byte;
    }

    public Byte get_Byte() {
        return _Byte;
    }

    public void set_Byte(Byte _Byte) {
        this._Byte = _Byte;
    }

    public byte[] get_byteArray() {
        return _byteArray;
    }

    public void set_byteArray(byte[] _byteArray) {
        this._byteArray = _byteArray;
    }

    public short get_short() {
        return _short;
    }

    public void set_short(short _short) {
        this._short = _short;
    }

    public Short get_Short() {
        return _Short;
    }

    public void set_Short(Short _Short) {
        this._Short = _Short;
    }

    public short[] get_shortArray() {
        return _shortArray;
    }

    public void set_shortArray(short[] _shortArray) {
        this._shortArray = _shortArray;
    }

    public int get_int() {
        return _int;
    }

    public void set_int(int _int) {
        this._int = _int;
    }

    public Integer get_Integer() {
        return _Integer;
    }

    public void set_Integer(Integer _Integer) {
        this._Integer = _Integer;
    }

    public int[] get_intArray() {
        return _intArray;
    }

    public void set_intArray(int[] _intArray) {
        this._intArray = _intArray;
    }

    public long get_long() {
        return _long;
    }

    public void set_long(long _long) {
        this._long = _long;
    }

    public Long get_Long() {
        return _Long;
    }

    public void set_Long(Long _Long) {
        this._Long = _Long;
    }

    public long[] get_longArray() {
        return _longArray;
    }

    public void set_longArray(long[] _longArray) {
        this._longArray = _longArray;
    }

    public float get_float() {
        return _float;
    }

    public void set_float(float _float) {
        this._float = _float;
    }

    public Float get_Float() {
        return _Float;
    }

    public void set_Float(Float _Float) {
        this._Float = _Float;
    }

    public float get_floatArray() {
        return _floatArray;
    }

    public void set_floatArray(float _floatArray) {
        this._floatArray = _floatArray;
    }

    public double get_double() {
        return _double;
    }

    public void set_double(double _double) {
        this._double = _double;
    }

    public Double get_Double() {
        return _Double;
    }

    public void set_Double(Double _Double) {
        this._Double = _Double;
    }

    public double[] get_doubleArray() {
        return _doubleArray;
    }

    public void set_doubleArray(double[] _doubleArray) {
        this._doubleArray = _doubleArray;
    }

    public char get_char() {
        return _char;
    }

    public void set_char(char _char) {
        this._char = _char;
    }

    public Character get_Character() {
        return _Character;
    }

    public void set_Character(Character _Character) {
        this._Character = _Character;
    }

    public char[] get_charArray() {
        return _charArray;
    }

    public void set_charArray(char[] _charArray) {
        this._charArray = _charArray;
    }

    public Date get_utilDate() {
        return _utilDate;
    }

    public void set_utilDate(Date _utilDate) {
        this._utilDate = _utilDate;
    }

    public java.sql.Date get_sqlDate() {
        return _sqlDate;
    }

    public void set_sqlDate(java.sql.Date _sqlDate) {
        this._sqlDate = _sqlDate;
    }

    public Time get_Time() {
        return _Time;
    }

    public void set_Time(Time _Time) {
        this._Time = _Time;
    }

    public Timestamp get_Timestamp() {
        return _Timestamp;
    }

    public void set_Timestamp(Timestamp _Timestamp) {
        this._Timestamp = _Timestamp;
    }

    public LocalTime get_LocalTime() {
        return _LocalTime;
    }

    public void set_LocalTime(LocalTime _LocalTime) {
        this._LocalTime = _LocalTime;
    }

    public LocalDateHandle get_LocalDateHandle() {
        return _LocalDateHandle;
    }

    public void set_LocalDateHandle(LocalDateHandle _LocalDateHandle) {
        this._LocalDateHandle = _LocalDateHandle;
    }

    public LocalDateTime get_LocalDateTime() {
        return _LocalDateTime;
    }

    public void set_LocalDateTime(LocalDateTime _LocalDateTime) {
        this._LocalDateTime = _LocalDateTime;
    }

    public Instant get_Instant() {
        return _Instant;
    }

    public void set_Instant(Instant _Instant) {
        this._Instant = _Instant;
    }

    public Duration get_Duration() {
        return _Duration;
    }

    public void set_Duration(Duration _Duration) {
        this._Duration = _Duration;
    }

    public Period get_Period() {
        return _Period;
    }

    public void set_Period(Period _Period) {
        this._Period = _Period;
    }

    public Year get_Year() {
        return _Year;
    }

    public void set_Year(Year _Year) {
        this._Year = _Year;
    }

    public YearMonth get_YearMonth() {
        return _YearMonth;
    }

    public void set_YearMonth(YearMonth _YearMonth) {
        this._YearMonth = _YearMonth;
    }

    public MonthDay get_MonthDay() {
        return _MonthDay;
    }

    public void set_MonthDay(MonthDay _MonthDay) {
        this._MonthDay = _MonthDay;
    }

    public OffsetDateTime get_OffsetDateTime() {
        return _OffsetDateTime;
    }

    public void set_OffsetDateTime(OffsetDateTime _OffsetDateTime) {
        this._OffsetDateTime = _OffsetDateTime;
    }

    public ZoneOffset get_ZoneOffset() {
        return _ZoneOffset;
    }

    public void set_ZoneOffset(ZoneOffset _ZoneOffset) {
        this._ZoneOffset = _ZoneOffset;
    }

    public OffsetTime get_OffsetTime() {
        return _OffsetTime;
    }

    public void set_OffsetTime(OffsetTime _OffsetTime) {
        this._OffsetTime = _OffsetTime;
    }

    public ZonedDateTime get_ZonedDateTime() {
        return _ZonedDateTime;
    }

    public void set_ZonedDateTime(ZonedDateTime _ZonedDateTime) {
        this._ZonedDateTime = _ZonedDateTime;
    }
}
