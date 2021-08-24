package com.jssh.netty.serial;

import com.jssh.netty.exception.SerialException;
import com.jssh.netty.serial.SerialHandler.InnerSerial;
import com.jssh.netty.support.MarshallingProperties;
import io.netty.buffer.ByteBuf;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class DefaultSerial implements MessageSerial {

    public static final String $_CLASS_NAME_$ = "$className$";
    public static final Charset default_charset = StandardCharsets.UTF_8;

    enum TYPE {
        NULL,
        BYTE,
        SHORT,
        INTEGER,
        LONG,
        FLOAT,
        DOUBLE,
        CHARACTER,
        BOOLEAN,

        PRIMITIVE_BYTE_ARRAY,
        PRIMITIVE_SHORT_ARRAY,
        PRIMITIVE_INT_ARRAY,
        PRIMITIVE_LONG_ARRAY,
        PRIMITIVE_FLOAT_ARRAY,
        PRIMITIVE_DOUBLE_ARRAY,
        PRIMITIVE_CHAR_ARRAY,
        PRIMITIVE_BOOLEAN_ARRAY,

        BYTE_ARRAY,
        SHORT_ARRAY,
        INTEGER_ARRAY,
        LONG_ARRAY,
        FLOAT_ARRAY,
        DOUBLE_ARRAY,
        CHARACTER_ARRAY,
        BOOLEAN_ARRAY,

        ARRAY_LIST,
        LINKED_LIST,
        HASH_SET,
        LINKED_HASH_SET,
        HASH_MAP,
        LINKED_HASH_MAP,
        HASH_TABLE,
        OBJECT_ARRAY,

        STRING,
        DATE,
        LOCAL_DATE,
        LOCAL_DATE_TIME,
        NETTY_FILE,
        BIG_DECIMAL,
        BIG_INTEGER,

        INNER_CUSTOM,
        CUSTOM
    }

    private static final Map<Class<?>, InnerSerialHandler> static_class_serials = new HashMap<>();
    private static final Map<Integer, InnerSerialHandler> static_type_serials = new HashMap<>();

    private static final Map<Class<?>, SerialHandler> ext_class_serials = new HashMap<>();
    private static final Map<Integer, SerialHandler> ext_type_serials = new HashMap<>();

    private final Map<Class<?>, InnerSerialHandler> class_serials = new HashMap<>();
    private final Map<Integer, InnerSerialHandler> type_serials = new HashMap<>();

    static {
        addStaticInnerSerial(Byte.class, TYPE.BYTE, new ByteSerial());
        addStaticInnerSerial(Short.class, TYPE.SHORT, new ShortSerial());
        addStaticInnerSerial(Integer.class, TYPE.INTEGER, new IntSerial());
        addStaticInnerSerial(Long.class, TYPE.LONG, new LongSerial());
        addStaticInnerSerial(Float.class, TYPE.FLOAT, new FloatSerial());
        addStaticInnerSerial(Double.class, TYPE.DOUBLE, new DoubleSerial());
        addStaticInnerSerial(Character.class, TYPE.CHARACTER, new CharacterSerial());
        addStaticInnerSerial(Boolean.class, TYPE.BOOLEAN, new BooleanSerial());

        addStaticInnerSerial(byte[].class, TYPE.PRIMITIVE_BYTE_ARRAY, new PrimitiveByteArraySerial());
        addStaticInnerSerial(short[].class, TYPE.PRIMITIVE_SHORT_ARRAY, new PrimitiveShortArraySerial());
        addStaticInnerSerial(int[].class, TYPE.PRIMITIVE_INT_ARRAY, new PrimitiveIntegerArraySerial());
        addStaticInnerSerial(long[].class, TYPE.PRIMITIVE_LONG_ARRAY, new PrimitiveLongArraySerial());
        addStaticInnerSerial(float[].class, TYPE.PRIMITIVE_FLOAT_ARRAY, new PrimitiveFloatArraySerial());
        addStaticInnerSerial(double[].class, TYPE.PRIMITIVE_DOUBLE_ARRAY, new PrimitiveDoubleArraySerial());
        addStaticInnerSerial(char[].class, TYPE.PRIMITIVE_CHAR_ARRAY, new PrimitiveCharArraySerial());
        addStaticInnerSerial(boolean[].class, TYPE.PRIMITIVE_BOOLEAN_ARRAY, new PrimitiveBooleanArraySerial());

        addStaticInnerSerial(Byte[].class, TYPE.BYTE_ARRAY, new ByteArraySerial());
        addStaticInnerSerial(Short[].class, TYPE.SHORT_ARRAY, new ShortArraySerial());
        addStaticInnerSerial(Integer[].class, TYPE.INTEGER_ARRAY, new IntegerArraySerial());
        addStaticInnerSerial(Long[].class, TYPE.LONG_ARRAY, new LongArraySerial());
        addStaticInnerSerial(Float[].class, TYPE.FLOAT_ARRAY, new FloatArraySerial());
        addStaticInnerSerial(Double[].class, TYPE.DOUBLE_ARRAY, new DoubleArraySerial());
        addStaticInnerSerial(Character[].class, TYPE.CHARACTER_ARRAY, new CharacterArraySerial());
        addStaticInnerSerial(Boolean[].class, TYPE.BOOLEAN_ARRAY, new BooleanArraySerial());

        addStaticInnerSerial(String.class, TYPE.STRING, new StringSerial());
        addStaticInnerSerial(Date.class, TYPE.DATE, new DateSerial());
        addStaticInnerSerial(LocalDate.class, TYPE.LOCAL_DATE, new LocalDateSerial());
        addStaticInnerSerial(LocalDateTime.class, TYPE.LOCAL_DATE_TIME, new LocalDateTimeSerial());
        addStaticInnerSerial(BigDecimal.class, TYPE.BIG_DECIMAL, new BigDecimalSerial());
        addStaticInnerSerial(BigInteger.class, TYPE.BIG_INTEGER, new BigIntegerSerial());
        addStaticInnerSerial(null, TYPE.NULL, new NullObjectSerial());
    }

    public DefaultSerial() {
        addInnerSerial(HashMap.class, TYPE.HASH_MAP, new MapSerial(HashMap.class));
        addInnerSerial(LinkedHashMap.class, TYPE.LINKED_HASH_MAP, new MapSerial(LinkedHashMap.class));
        addInnerSerial(Hashtable.class, TYPE.HASH_TABLE, new MapSerial(Hashtable.class));
        addInnerSerial(ArrayList.class, TYPE.ARRAY_LIST, new CollectionSerial(ArrayList.class));
        addInnerSerial(LinkedList.class, TYPE.LINKED_LIST, new CollectionSerial(LinkedList.class));
        addInnerSerial(HashSet.class, TYPE.HASH_SET, new CollectionSerial(HashSet.class));
        addInnerSerial(LinkedHashSet.class, TYPE.LINKED_HASH_SET, new CollectionSerial(LinkedHashSet.class));
        addInnerSerial(Object[].class, TYPE.OBJECT_ARRAY, new ObjectArraySerial());

        addInnerSerial(Void.class, TYPE.INNER_CUSTOM, new InnerCustomSerial());
    }

    protected static void addStaticInnerSerial(Class<?> jClass, TYPE type, InnerSerial serial) {
        InnerSerialHandler innerSerialHandler = new InnerSerialHandler(type, serial);
        static_class_serials.put(jClass, innerSerialHandler);
        static_type_serials.put(type.ordinal(), innerSerialHandler);
    }

    protected void addInnerSerial(Class<?> jClass, TYPE type, InnerSerial serial) {
        InnerSerialHandler innerSerialHandler = new InnerSerialHandler(type, serial);
        class_serials.put(jClass, innerSerialHandler);
        type_serials.put(type.ordinal(), innerSerialHandler);
    }

    protected void addExtInnerSerialHandler(Class<?> jClass, SerialHandler serialHandler) {
        assert serialHandler.getType() < 0;
        ext_class_serials.put(jClass, serialHandler);
        ext_type_serials.put(serialHandler.getType(), serialHandler);
    }

    public SerialHandler getSerial(Class<?> jClass) {
        InnerSerialHandler handler = static_class_serials.get(jClass);
        if (handler != null) {
            return handler;
        }
        handler = class_serials.get(jClass);
        if (handler != null) {
            return handler;
        }
        return ext_class_serials.get(jClass);
    }

    public SerialHandler getSerial(Integer type) {
        InnerSerialHandler handler = static_type_serials.get(type);
        if (handler != null) {
            return handler;
        }
        handler = type_serials.get(type);
        if (handler != null) {
            return handler;
        }
        return ext_type_serials.get(type);
    }

    @Override
    public void serialize(ByteBuf buf, Object object) throws Exception {
        writeObject(buf, object);
    }

    protected void writeObject(ByteBuf buf, Object object) throws Exception {
        if (object == null) {
            buf.writeByte(TYPE.NULL.ordinal());
            return;
        }
        SerialHandler serialHandler = getSerial(object.getClass());

        if (serialHandler == null) {
            serialHandler = getGeneralSerial(object);
        }

        if (serialHandler == null) {
            buf.markWriterIndex();
            buf.writeByte(TYPE.CUSTOM.ordinal());
            boolean write = writeByCustomSerial(buf, object);
            if (write) {
                return;
            }
            buf.resetWriterIndex();
        }

        if (serialHandler == null) {
            serialHandler = getInnerCustomSerial(object);
        }

        if (serialHandler == null) {
            throw new SerialException("Can not find the serial handler : " + object);
        }
        buf.writeByte(serialHandler.getType());
        serialHandler.getInnerSerial().write(buf, object);
    }

    protected SerialHandler getGeneralSerial(Object obj) {
        if (obj instanceof CharSequence) {
            return getSerial(TYPE.STRING.ordinal());
        } else if (obj instanceof Date) {
            return getSerial(TYPE.DATE.ordinal());
        } else if (obj instanceof Map) {
            return getSerial(TYPE.HASH_MAP.ordinal());
        } else if (obj instanceof Set) {
            return getSerial(TYPE.HASH_SET.ordinal());
        } else if (obj instanceof Collection) {
            return getSerial(TYPE.ARRAY_LIST.ordinal());
        } else {
            return null;
        }
    }

    protected boolean writeByCustomSerial(ByteBuf buf, Object obj) {
        return false;
    }

    protected Object readByCustomSerial(ByteBuf buf) {
        return null;
    }

    private SerialHandler getInnerCustomSerial(Object obj) {
        String className = obj.getClass().getName();
        if (className.startsWith("java.") || className.startsWith("[")) {
            throw new SerialException("Unsupported data type : " + className);
        }
        return getSerial(TYPE.INNER_CUSTOM.ordinal());
    }

    @Override
    public Object deSerialize(ByteBuf buf) throws Exception {
        return readObject(buf);
    }

    protected Object readObject(ByteBuf in) throws Exception {
        byte ordinal = in.readByte();
        if (ordinal >= 0 && ordinal < TYPE.values().length) {
            if (TYPE.values()[ordinal] == TYPE.NULL) {
                return null;
            }
            if (TYPE.values()[ordinal] == TYPE.CUSTOM) {
                return readByCustomSerial(in);
            }
        }

        SerialHandler serialHandler = getSerial((int) ordinal);

        if (serialHandler == null) {
            throw new SerialException("Unsupported serial type " + ordinal);
        }

        return serialHandler.getInnerSerial().read(in);
    }

    private Object newObject(String className, Map<Object, Object> properties) throws Exception {
        Class<?> clazz = getClass(className);
        if (clazz != null) {
            Object obj = clazz.newInstance();
            copyProperties(obj, properties);
            return obj;
        } else {
            if (properties != null) {
                properties.put($_CLASS_NAME_$, className);
            }
            return properties;
        }
    }

    private Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Map<String, Object> toMap(Object obj) throws Exception {
        Map<String, Object> map = new HashMap<>();
        BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor property : propertyDescriptors) {
            String key = property.getName();
            if (!key.equals("class")) {
                // 得到property对应的getter方法
                Method getter = property.getReadMethod();
                Object value = getter.invoke(obj);
                map.put(key, value);
            }
        }
        return map;
    }

    private static void copyProperties(Object obj, Map<Object, Object> properties) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor property : propertyDescriptors) {
            String key = property.getName();
            if (!key.equals("class") && properties.containsKey(key)) {
                Method setter = property.getWriteMethod();
                setter.invoke(obj, properties.get(key));
            }
        }
    }

    static class NullObjectSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
        }

        @Override
        public Object read(ByteBuf buf) {
            return null;
        }
    }

    static class ByteSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            buf.writeByte((Byte) obj);
        }

        @Override
        public Object read(ByteBuf buf) {
            return buf.readByte();
        }
    }

    static class ShortSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            buf.writeShort((Short) obj);
        }

        @Override
        public Object read(ByteBuf buf) {
            return buf.readShort();
        }
    }

    static class IntSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            buf.writeInt((Integer) obj);
        }

        @Override
        public Object read(ByteBuf buf) {
            return buf.readInt();
        }
    }

    static class BooleanSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            buf.writeBoolean((Boolean) obj);
        }

        @Override
        public Object read(ByteBuf buf) {
            return buf.readBoolean();
        }
    }

    static class LongSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            buf.writeLong((Long) obj);
        }

        @Override
        public Object read(ByteBuf buf) {
            return buf.readLong();
        }
    }

    static class DoubleSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            buf.writeDouble((Double) obj);
        }

        @Override
        public Object read(ByteBuf buf) {
            return buf.readDouble();
        }
    }

    static class FloatSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            buf.writeFloat((Float) obj);
        }

        @Override
        public Object read(ByteBuf buf) {
            return buf.readFloat();
        }
    }

    static class StringSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            String value = (String) obj;
            int lenIndex = buf.writerIndex();
            buf.writeInt(0);
            int len = buf.writeCharSequence(value, default_charset);
            buf.markWriterIndex();
            buf.setInt(lenIndex, len);
            buf.resetWriterIndex();
        }

        @Override
        public Object read(ByteBuf buf) {
            return buf.readCharSequence(buf.readInt(), default_charset).toString();
        }
    }

    static class CharacterSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            buf.writeChar((Character) obj);
        }

        @Override
        public Object read(ByteBuf buf) {
            return buf.readChar();
        }
    }

    static class BigDecimalSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            String value = obj.toString();
            int lenIndex = buf.writerIndex();
            buf.writeInt(0);
            int len = buf.writeCharSequence(value, default_charset);
            buf.markWriterIndex();
            buf.setInt(lenIndex, len);
            buf.resetWriterIndex();
        }

        @Override
        public Object read(ByteBuf buf) {
            return new BigDecimal(buf.readCharSequence(buf.readInt(), default_charset).toString());
        }
    }

    static class BigIntegerSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            String value = obj.toString();
            int lenIndex = buf.writerIndex();
            buf.writeInt(0);
            int len = buf.writeCharSequence(value, default_charset);
            buf.markWriterIndex();
            buf.setInt(lenIndex, len);
            buf.resetWriterIndex();
        }

        @Override
        public Object read(ByteBuf buf) {
            return new BigInteger(buf.readCharSequence(buf.readInt(), default_charset).toString());
        }
    }

    static class PrimitiveShortArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            short[] value = (short[]) obj;
            buf.writeInt(value.length);
            for (short i : value) {
                buf.writeShort(i);
            }
        }

        @Override
        public Object read(ByteBuf buf) {
            short[] values = new short[buf.readInt()];
            for (int i = 0, size = values.length; i < size; i++) {
                values[i] = buf.readShort();
            }
            return values;
        }
    }

    static class PrimitiveIntegerArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            int[] value = (int[]) obj;
            buf.writeInt(value.length);
            for (int i : value) {
                buf.writeInt(i);
            }
        }

        @Override
        public Object read(ByteBuf buf) {
            int[] values = new int[buf.readInt()];
            for (int i = 0, size = values.length; i < size; i++) {
                values[i] = buf.readInt();
            }
            return values;
        }
    }

    static class PrimitiveLongArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            long[] value = (long[]) obj;
            buf.writeInt(value.length);
            for (long i : value) {
                buf.writeLong(i);
            }
        }

        @Override
        public Object read(ByteBuf buf) {
            long[] values = new long[buf.readInt()];
            for (int i = 0, size = values.length; i < size; i++) {
                values[i] = buf.readLong();
            }
            return values;
        }
    }

    static class PrimitiveFloatArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            float[] value = (float[]) obj;
            buf.writeInt(value.length);
            for (float i : value) {
                buf.writeFloat(i);
            }
        }

        @Override
        public Object read(ByteBuf buf) {
            float[] values = new float[buf.readInt()];
            for (int i = 0, size = values.length; i < size; i++) {
                values[i] = buf.readFloat();
            }
            return values;
        }
    }

    static class PrimitiveDoubleArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            double[] value = (double[]) obj;
            buf.writeInt(value.length);
            for (double i : value) {
                buf.writeDouble(i);
            }
        }

        @Override
        public Object read(ByteBuf buf) {
            double[] values = new double[buf.readInt()];
            for (int i = 0, size = values.length; i < size; i++) {
                values[i] = buf.readDouble();
            }
            return values;
        }
    }

    static class PrimitiveCharArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            char[] value = (char[]) obj;
            buf.writeInt(value.length);
            for (char i : value) {
                buf.writeChar(i);
            }
        }

        @Override
        public Object read(ByteBuf buf) {
            char[] values = new char[buf.readInt()];
            for (int i = 0, size = values.length; i < size; i++) {
                values[i] = buf.readChar();
            }
            return values;
        }
    }

    static class PrimitiveBooleanArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            boolean[] value = (boolean[]) obj;
            buf.writeInt(value.length);
            for (boolean i : value) {
                buf.writeBoolean(i);
            }
        }

        @Override
        public Object read(ByteBuf buf) {
            boolean[] values = new boolean[buf.readInt()];
            for (int i = 0, size = values.length; i < size; i++) {
                values[i] = buf.readBoolean();
            }
            return values;
        }
    }

    static class ByteArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            Byte[] value = (Byte[]) obj;
            buf.writeInt(value.length);
            for (byte i : value) {
                buf.writeByte(i);
            }
        }

        @Override
        public Object read(ByteBuf buf) {
            Byte[] byteArray = new Byte[buf.readInt()];
            for (int i = 0, size = byteArray.length; i < size; i++) {
                byteArray[i] = buf.readByte();
            }
            return byteArray;
        }
    }

    static class ShortArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            Short[] value = (Short[]) obj;
            buf.writeInt(value.length);
            for (short i : value) {
                buf.writeShort(i);
            }
        }

        @Override
        public Object read(ByteBuf buf) {
            Short[] values = new Short[buf.readInt()];
            for (int i = 0, size = values.length; i < size; i++) {
                values[i] = buf.readShort();
            }
            return values;
        }
    }

    static class IntegerArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            Integer[] value = (Integer[]) obj;
            buf.writeInt(value.length);
            for (int i : value) {
                buf.writeInt(i);
            }
        }

        @Override
        public Object read(ByteBuf buf) {
            Integer[] values = new Integer[buf.readInt()];
            for (int i = 0, size = values.length; i < size; i++) {
                values[i] = buf.readInt();
            }
            return values;
        }
    }

    static class LongArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            Long[] value = (Long[]) obj;
            buf.writeInt(value.length);
            for (long i : value) {
                buf.writeLong(i);
            }
        }

        @Override
        public Object read(ByteBuf buf) {
            Long[] values = new Long[buf.readInt()];
            for (int i = 0, size = values.length; i < size; i++) {
                values[i] = buf.readLong();
            }
            return values;
        }
    }

    static class FloatArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            Float[] value = (Float[]) obj;
            buf.writeInt(value.length);
            for (float i : value) {
                buf.writeFloat(i);
            }
        }

        @Override
        public Object read(ByteBuf buf) {
            Float[] values = new Float[buf.readInt()];
            for (int i = 0, size = values.length; i < size; i++) {
                values[i] = buf.readFloat();
            }
            return values;
        }
    }

    static class DoubleArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            Double[] value = (Double[]) obj;
            buf.writeInt(value.length);
            for (double i : value) {
                buf.writeDouble(i);
            }
        }

        @Override
        public Object read(ByteBuf buf) {
            Double[] values = new Double[buf.readInt()];
            for (int i = 0, size = values.length; i < size; i++) {
                values[i] = buf.readDouble();
            }
            return values;
        }
    }

    static class CharacterArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            Character[] value = (Character[]) obj;
            buf.writeInt(value.length);
            for (char i : value) {
                buf.writeChar(i);
            }
        }

        @Override
        public Object read(ByteBuf buf) {
            Character[] values = new Character[buf.readInt()];
            for (int i = 0, size = values.length; i < size; i++) {
                values[i] = buf.readChar();
            }
            return values;
        }
    }

    static class BooleanArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            Boolean[] value = (Boolean[]) obj;
            buf.writeInt(value.length);
            for (boolean i : value) {
                buf.writeBoolean(i);
            }
        }

        @Override
        public Object read(ByteBuf buf) {
            Boolean[] values = new Boolean[buf.readInt()];
            for (int i = 0, size = values.length; i < size; i++) {
                values[i] = buf.readBoolean();
            }
            return values;
        }
    }

    static class DateSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            long value = ((Date) obj).getTime();
            buf.writeLong(value);
        }

        @Override
        public Object read(ByteBuf buf) {
            return new Date(buf.readLong());
        }
    }

    static class LocalDateSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            long value = ((LocalDate) obj).toEpochDay();
            buf.writeLong(value);
        }

        @Override
        public Object read(ByteBuf buf) {
            return LocalDate.ofEpochDay(buf.readLong());
        }
    }

    static class LocalDateTimeSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            long value = ((LocalDateTime) obj).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            buf.writeLong(value);
        }

        @Override
        public Object read(ByteBuf buf) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(buf.readLong()), ZoneId.systemDefault());
        }
    }

    static class PrimitiveByteArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            byte[] value = (byte[]) obj;
            buf.writeInt(value.length);
            buf.writeBytes(value);
        }

        @Override
        public Object read(ByteBuf buf) {
            int len = buf.readInt();
            byte[] byteArray = new byte[len];
            buf.readBytes(byteArray);
            return byteArray;
        }
    }

    class ObjectArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) throws Exception {
            Object[] value = (Object[]) obj;
            buf.writeInt(value.length);
            for (Object val : value) {
                writeObject(buf, val);
            }
        }

        @Override
        public Object read(ByteBuf buf) throws Exception {
            Object[] objectArray = new Object[buf.readInt()];
            for (int i = 0, size = objectArray.length; i < size; i++) {
                objectArray[i] = readObject(buf);
            }
            return objectArray;
        }
    }

    class MapSerial implements InnerSerial {

        private final Class<? extends Map> jClass;

        public MapSerial(Class<? extends Map> jClass) {
            this.jClass = jClass;
        }

        @Override
        public void write(ByteBuf buf, Object obj) throws Exception {
            Map<?, ?> value = (Map<?, ?>) obj;
            buf.writeInt(value.size());
            for (Map.Entry<?, ?> entry : value.entrySet()) {
                writeObject(buf, entry.getKey());
                writeObject(buf, entry.getValue());
            }
        }

        @Override
        public Object read(ByteBuf buf) throws Exception {
            Map<Object, Object> mapValue = jClass.newInstance();
            for (int i = 0, size = buf.readInt(); i < size; i++) {
                mapValue.put(readObject(buf), readObject(buf));
            }
            if (mapValue.containsKey($_CLASS_NAME_$)) {
                String className = (String) mapValue.get($_CLASS_NAME_$);
                return newObject(className, mapValue);
            }
            return mapValue;
        }
    }

    class CollectionSerial implements InnerSerial {

        private final Class<? extends Collection> jClass;

        public CollectionSerial(Class<? extends Collection> jClass) {
            this.jClass = jClass;
        }

        @Override
        public void write(ByteBuf buf, Object obj) throws Exception {
            Collection<?> value = (Collection<?>) obj;
            buf.writeInt(value.size());
            for (Object val : value) {
                writeObject(buf, val);
            }
        }

        @Override
        public Object read(ByteBuf buf) throws Exception {
            Collection<Object> collection = jClass.newInstance();
            for (int i = 0, size = buf.readInt(); i < size; i++) {
                collection.add(readObject(buf));
            }
            return collection;
        }
    }

    class InnerCustomSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) throws Exception {

            MarshallingProperties mp = null;
            Class<?> clazz = obj.getClass();
            while (mp == null && clazz != null) {
                mp = clazz.getAnnotation(MarshallingProperties.class);
                clazz = clazz.getSuperclass();
            }

            String className = mp != null && !mp.className().isEmpty() ? mp.className()
                    : obj.getClass().getName();

            writeObject(buf, className);

            Map<String, Object> value = toMap(obj);

            if (mp != null && mp.value().length > 0) {
                List<String> exist = Arrays.asList(mp.value());
                for (String key : value.keySet()) {
                    if (!exist.contains(key)) {
                        value.remove(key);
                    }
                }
            }

            if (mp != null && mp.ignoreProperties().length > 0) {
                for (String ig : mp.ignoreProperties()) {
                    value.remove(ig);
                }
            }
            writeObject(buf, value);
        }

        @Override
        public Object read(ByteBuf buf) throws Exception {
            String className = (String) readObject(buf);
            Map<Object, Object> value = (Map<Object, Object>) readObject(buf);
            return newObject(className, value);
        }
    }

    static class InnerSerialHandler implements SerialHandler {

        private final TYPE type;

        private final InnerSerial innerSerial;

        public InnerSerialHandler(TYPE type, InnerSerial innerSerial) {
            this.type = type;
            this.innerSerial = innerSerial;
        }

        @Override
        public int getType() {
            return type.ordinal();
        }

        @Override
        public InnerSerial getInnerSerial() {
            return innerSerial;
        }
    }
}
