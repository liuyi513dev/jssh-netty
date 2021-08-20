package com.jssh.netty.serial;

import com.jssh.netty.exception.SerialException;
import com.jssh.netty.support.MarshallingProperties;
import io.netty.buffer.ByteBuf;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
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

public class DefaultSerial extends ChunkFileMessageSerial implements MessageSerial {

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
        CHUNKED_FILE,
        BIG_DECIMAL,
        BIG_INTEGER,

        CUSTOM
    }

    interface InnerSerial {
        void write(ByteBuf buf, Object obj) throws Exception;

        Object read(ByteBuf buf) throws Exception;
    }

    public static Map<Class<?>, TYPE> static_types = new HashMap<>();
    public static Map<TYPE, InnerSerial> static_serials = new HashMap<>();

    public Map<Class<?>, TYPE> types = new HashMap<>();
    public Map<TYPE, InnerSerial> serials = new HashMap<>();

    static {
        addStaticSerial(Byte.class, TYPE.BYTE, new ByteSerial());
        addStaticSerial(Short.class, TYPE.SHORT, new ShortSerial());
        addStaticSerial(Integer.class, TYPE.INTEGER, new IntSerial());
        addStaticSerial(Long.class, TYPE.LONG, new LongSerial());
        addStaticSerial(Float.class, TYPE.FLOAT, new FloatSerial());
        addStaticSerial(Double.class, TYPE.DOUBLE, new DoubleSerial());
        addStaticSerial(Character.class, TYPE.CHARACTER, new CharacterSerial());
        addStaticSerial(Boolean.class, TYPE.BOOLEAN, new BooleanSerial());

        addStaticSerial(byte[].class, TYPE.PRIMITIVE_BYTE_ARRAY, new PrimitiveByteArraySerial());
        addStaticSerial(short[].class, TYPE.PRIMITIVE_SHORT_ARRAY, new PrimitiveShortArraySerial());
        addStaticSerial(int[].class, TYPE.PRIMITIVE_INT_ARRAY, new PrimitiveIntegerArraySerial());
        addStaticSerial(long[].class, TYPE.PRIMITIVE_LONG_ARRAY, new PrimitiveLongArraySerial());
        addStaticSerial(float[].class, TYPE.PRIMITIVE_FLOAT_ARRAY, new PrimitiveFloatArraySerial());
        addStaticSerial(double[].class, TYPE.PRIMITIVE_DOUBLE_ARRAY, new PrimitiveDoubleArraySerial());
        addStaticSerial(char[].class, TYPE.PRIMITIVE_CHAR_ARRAY, new PrimitiveCharArraySerial());
        addStaticSerial(boolean[].class, TYPE.PRIMITIVE_BOOLEAN_ARRAY, new PrimitiveBooleanArraySerial());

        addStaticSerial(Byte[].class, TYPE.SHORT_ARRAY, new ByteArraySerial());
        addStaticSerial(Short[].class, TYPE.SHORT_ARRAY, new ShortArraySerial());
        addStaticSerial(Integer[].class, TYPE.INTEGER_ARRAY, new IntegerArraySerial());
        addStaticSerial(Long[].class, TYPE.LONG_ARRAY, new LongArraySerial());
        addStaticSerial(Float[].class, TYPE.FLOAT_ARRAY, new FloatArraySerial());
        addStaticSerial(Double[].class, TYPE.DOUBLE_ARRAY, new DoubleArraySerial());
        addStaticSerial(Character[].class, TYPE.CHARACTER_ARRAY, new CharacterArraySerial());
        addStaticSerial(Boolean[].class, TYPE.BOOLEAN_ARRAY, new BooleanArraySerial());

        addStaticSerial(String.class, TYPE.STRING, new StringSerial());
        addStaticSerial(Date.class, TYPE.DATE, new DateSerial());
        addStaticSerial(LocalDate.class, TYPE.LOCAL_DATE, new LocalDateSerial());
        addStaticSerial(LocalDateTime.class, TYPE.LOCAL_DATE_TIME, new LocalDateTimeSerial());
        addStaticSerial(BigDecimal.class, TYPE.BIG_DECIMAL, new BigDecimalSerial());
        addStaticSerial(BigInteger.class, TYPE.BIG_INTEGER, new BigIntegerSerial());
    }

    public DefaultSerial() {
        addSerial(HashMap.class, TYPE.HASH_MAP, new MapSerial(TYPE.HASH_MAP, HashMap.class));
        addSerial(LinkedHashMap.class, TYPE.LINKED_HASH_MAP, new MapSerial(TYPE.LINKED_HASH_MAP, LinkedHashMap.class));
        addSerial(Hashtable.class, TYPE.HASH_TABLE, new MapSerial(TYPE.HASH_TABLE, Hashtable.class));
        addSerial(ArrayList.class, TYPE.ARRAY_LIST, new CollectionSerial(TYPE.ARRAY_LIST, ArrayList.class));
        addSerial(LinkedList.class, TYPE.LINKED_LIST, new CollectionSerial(TYPE.LINKED_LIST, LinkedList.class));
        addSerial(HashSet.class, TYPE.HASH_SET, new CollectionSerial(TYPE.HASH_SET, HashSet.class));
        addSerial(LinkedHashSet.class, TYPE.LINKED_HASH_SET, new CollectionSerial(TYPE.LINKED_HASH_SET, LinkedHashSet.class));
        addSerial(Object[].class, TYPE.OBJECT_ARRAY, new ObjectArraySerial());
        addSerial(ChunkFile.class, TYPE.CHUNKED_FILE, new ChunkFileSerial());

        addSerial(Void.class, TYPE.CUSTOM, new CustomSerial());
    }

    public static void addStaticSerial(Class<?> jClass, TYPE type, InnerSerial serial) {
        static_types.put(jClass, type);
        static_serials.put(type, serial);
    }

    public void addSerial(Class<?> jClass, TYPE type, InnerSerial serial) {
        types.put(jClass, type);
        serials.put(type, serial);
    }

    public InnerSerial getSerial(Class<?> jClass) {
        TYPE type = static_types.get(jClass);
        if (type != null) {
            return static_serials.get(type);
        }
        type = types.get(jClass);
        if (type != null) {
            return serials.get(type);
        }
        return null;
    }

    public InnerSerial getSerial(TYPE type) {
        InnerSerial serial = static_serials.get(type);
        if (serial != null) {
            return serial;
        }
        return serials.get(type);
    }

    @Override
    public boolean support(Object body) {
        return true;
    }

    @Override
    public void serialize(ByteBuf buf, Object value) throws Exception {
        writeObject(buf, value);
    }

    private void writeObject(ByteBuf buf, Object obj) throws Exception {
        if (obj == null) {
            buf.writeByte(TYPE.NULL.ordinal());
            return;
        }

        InnerSerial innerSerial = getSerial(obj.getClass());

        if (innerSerial == null) {
            if (obj instanceof CharSequence) {
                innerSerial = getSerial(TYPE.STRING);
            } else if (obj instanceof Date) {
                innerSerial = getSerial(TYPE.DATE);
            } else if (obj instanceof Map) {
                innerSerial = getSerial(TYPE.HASH_MAP);
            } else if (obj instanceof Set) {
                innerSerial = getSerial(TYPE.HASH_SET);
            } else if (obj instanceof Collection) {
                innerSerial = getSerial(TYPE.ARRAY_LIST);
            } else {
                String className = obj.getClass().getName();
                if (className.startsWith("java.") || className.startsWith("[")) {
                    throw new SerialException("Unsupported data type : " + className);
                }
                innerSerial = getSerial(TYPE.CUSTOM);
            }
        }

        innerSerial.write(buf, obj);
    }

    @Override
    public Object deSerialize(ByteBuf buf) throws Exception {
        return readObject(buf);
    }

    private Object readObject(ByteBuf in) throws Exception {
        byte ordinal = in.readByte();

        TYPE[] types = TYPE.values();

        if (ordinal >= types.length) {
            throw new SerialException("Unsupported serial type " + ordinal);
        }

        TYPE type = types[ordinal];

        if (type == TYPE.NULL) {
            return null;
        }

        InnerSerial innerSerial = getSerial(type);

        if (innerSerial == null) {
            throw new SerialException("Unsupported serial type " + type);
        }

        return innerSerial.read(in);
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

    static class ByteSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            buf.writeByte(TYPE.BYTE.ordinal());
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
            buf.writeByte(TYPE.SHORT.ordinal());
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
            buf.writeByte(TYPE.INTEGER.ordinal());
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
            buf.writeByte(TYPE.BOOLEAN.ordinal());
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
            buf.writeByte(TYPE.LONG.ordinal());
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
            buf.writeByte(TYPE.DOUBLE.ordinal());
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
            buf.writeByte(TYPE.FLOAT.ordinal());
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
            buf.writeByte(TYPE.STRING.ordinal());
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
            buf.writeByte(TYPE.CHARACTER.ordinal());
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
            buf.writeByte(TYPE.BIG_DECIMAL.ordinal());
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
            buf.writeByte(TYPE.BIG_INTEGER.ordinal());
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
            buf.writeByte(TYPE.PRIMITIVE_SHORT_ARRAY.ordinal());
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
            buf.writeByte(TYPE.PRIMITIVE_INT_ARRAY.ordinal());
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
            buf.writeByte(TYPE.PRIMITIVE_LONG_ARRAY.ordinal());
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
            buf.writeByte(TYPE.PRIMITIVE_FLOAT_ARRAY.ordinal());
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
            buf.writeByte(TYPE.PRIMITIVE_DOUBLE_ARRAY.ordinal());
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
            buf.writeByte(TYPE.PRIMITIVE_CHAR_ARRAY.ordinal());
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
            buf.writeByte(TYPE.PRIMITIVE_BOOLEAN_ARRAY.ordinal());
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
            buf.writeByte(TYPE.BYTE_ARRAY.ordinal());
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
            buf.writeByte(TYPE.SHORT_ARRAY.ordinal());
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
            buf.writeByte(TYPE.INTEGER_ARRAY.ordinal());
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
            buf.writeByte(TYPE.LONG_ARRAY.ordinal());
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
            buf.writeByte(TYPE.FLOAT_ARRAY.ordinal());
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
            buf.writeByte(TYPE.DOUBLE_ARRAY.ordinal());
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
            buf.writeByte(TYPE.CHARACTER_ARRAY.ordinal());
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
            buf.writeByte(TYPE.BOOLEAN_ARRAY.ordinal());
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
            buf.writeByte(TYPE.DATE.ordinal());
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
            buf.writeByte(TYPE.LOCAL_DATE.ordinal());
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
            buf.writeByte(TYPE.LOCAL_DATE_TIME.ordinal());
            long value = ((LocalDateTime) obj).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            buf.writeLong(value);
        }

        @Override
        public Object read(ByteBuf buf) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(buf.readLong()), ZoneId.systemDefault());
        }
    }


    class ChunkFileSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            buf.writeByte(TYPE.CHUNKED_FILE.ordinal());
            buf.writeLong(((ChunkFile) obj).getLength());
            addSerChunkFile((ChunkFile) obj);
        }

        @Override
        public Object read(ByteBuf buf) throws IOException {
            long length = buf.readLong();
            ChunkFile chunkFile = createChunkFile(length);
            addDeSerChunkFile(chunkFile);
            return chunkFile;
        }
    }

    static class PrimitiveByteArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            buf.writeByte(TYPE.PRIMITIVE_BYTE_ARRAY.ordinal());
            byte[] value = (byte[]) obj;
            buf.writeInt(value.length);
            buf.writeBytes(value);
        }

        @Override
        public Object read(ByteBuf buf) throws IOException {
            int len = buf.readInt();
            byte[] byteArray = new byte[len];
            buf.readBytes(byteArray);
            return byteArray;
        }
    }

    class ObjectArraySerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) throws Exception {
            buf.writeByte(TYPE.OBJECT_ARRAY.ordinal());
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

        private final TYPE type;
        private final Class<? extends Map> jClass;

        public MapSerial(TYPE type, Class<? extends Map> jClass) {
            this.type = type;
            this.jClass = jClass;
        }

        @Override
        public void write(ByteBuf buf, Object obj) throws Exception {
            buf.writeByte(type.ordinal());
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

        private final TYPE type;
        private final Class<? extends Collection> jClass;

        public CollectionSerial(TYPE type, Class<? extends Collection> jClass) {
            this.type = type;
            this.jClass = jClass;
        }

        @Override
        public void write(ByteBuf buf, Object obj) throws Exception {
            buf.writeByte(type.ordinal());
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

    class CustomSerial implements InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) throws Exception {
            buf.writeByte(TYPE.CUSTOM.ordinal());

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
}
