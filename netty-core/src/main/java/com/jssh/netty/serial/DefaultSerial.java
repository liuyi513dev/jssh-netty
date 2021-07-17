package com.jssh.netty.serial;

import com.jssh.netty.support.MarshallingProperties;
import io.netty.buffer.ByteBuf;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;

public class DefaultSerial extends ChunkFileMessageSerial implements MessageSerial {

    public static final String $_CLASS_NAME_$ = "$className$";
    private Charset charset = Charset.forName("UTF-8");

    public static final byte TYPE_NULL = 0;
    public static final byte TYPE_BYTE = 1;
    public static final byte TYPE_INTEGER = 2;
    public static final byte TYPE_BOOLEAN = 3;
    public static final byte TYPE_LONG = 4;
    public static final byte TYPE_DOUBLE = 5;
    public static final byte TYPE_STRING = 6;
    public static final byte TYPE_DATE = 7;
    public static final byte TYPE_MAP = 8;
    public static final byte TYPE_LIST = 9;
    public static final byte TYPE_BYTE_ARRAY = 10;
    public static final byte TYPE_CUSTOM = 11;
    public static final byte TYPE_OBJECT_ARRAY = 12;
    public static final byte TYPE_CHUNKEDFILE = 13;
    public static final byte TYPE_SET = 14;
    public static final byte TYPE_FLOAT = 15;
    public static final byte TYPE_COLLECTION = 16;

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
            buf.writeByte(TYPE_NULL);
            return;
        }
        if (obj instanceof Byte) {
            buf.writeByte(TYPE_BYTE);
            buf.writeByte((Byte) obj);
        } else if (obj instanceof Integer) {
            buf.writeByte(TYPE_INTEGER);
            buf.writeInt((Integer) obj);
        } else if (obj instanceof Boolean) {
            buf.writeByte(TYPE_BOOLEAN);
            buf.writeBoolean((Boolean) obj);
        } else if (obj instanceof Long) {
            buf.writeByte(TYPE_LONG);
            buf.writeLong((Long) obj);
        } else if (obj instanceof Double) {
            buf.writeByte(TYPE_DOUBLE);
            buf.writeDouble((Double) obj);
        } else if (obj instanceof Float) {
            buf.writeByte(TYPE_FLOAT);
            buf.writeFloat((Float) obj);
        } else if (obj instanceof String) {
            buf.writeByte(TYPE_STRING);
            byte[] value = ((String) obj).getBytes(charset);
            buf.writeInt(value.length);
            buf.writeBytes(value);
        } else if (obj instanceof Date) {
            buf.writeByte(TYPE_DATE);
            long value = ((Date) obj).getTime();
            buf.writeLong(value);
        } else if (obj instanceof Map) {
            buf.writeByte(TYPE_MAP);
            Map<?, ?> value = (Map<?, ?>) obj;
            buf.writeInt(value.size());
            for (Map.Entry<?, ?> entry : value.entrySet()) {
                writeObject(buf, entry.getKey());
                writeObject(buf, entry.getValue());
            }
        } else if (obj instanceof List) {
            buf.writeByte(TYPE_LIST);
            List<?> value = (List<?>) obj;
            buf.writeInt(value.size());
            for (Object val : value) {
                writeObject(buf, val);
            }
        } else if (obj instanceof Set) {
            buf.writeByte(TYPE_SET);
            Set<?> value = (Set<?>) obj;
            buf.writeInt(value.size());
            for (Object val : value) {
                writeObject(buf, val);
            }
        } else if (obj instanceof Collection) {
            buf.writeByte(TYPE_COLLECTION);
            Collection<?> value = (Collection<?>) obj;
            buf.writeInt(value.size());
            for (Object val : value) {
                writeObject(buf, val);
            }
        } else if (obj instanceof byte[]) {
            buf.writeByte(TYPE_BYTE_ARRAY);
            byte[] value = (byte[]) obj;
            buf.writeInt(value.length);
            buf.writeBytes(value);
        } else if (obj instanceof Object[]) {
            buf.writeByte(TYPE_OBJECT_ARRAY);
            Object[] value = (Object[]) obj;
            buf.writeInt(value.length);
            for (Object val : value) {
                writeObject(buf, val);
            }
        } else if (obj instanceof ChunkFile) {
            buf.writeByte(TYPE_CHUNKEDFILE);
            buf.writeLong(((ChunkFile) obj).getLength());
            addSerChunkFile((ChunkFile) obj);
        } else {
            buf.writeByte(TYPE_CUSTOM);

            MarshallingProperties mp = null;
            Class<?> clazz = obj.getClass();
            while (mp == null && clazz != null) {
                mp = clazz.getAnnotation(MarshallingProperties.class);
                clazz = clazz.getSuperclass();
            }

            String className = mp != null && mp.className() != null && !mp.className().isEmpty() ? mp.className()
                    : obj.getClass().getName();

            writeObject(buf, obj.getClass().getSimpleName());
            writeObject(buf, className);

            Map<String, Object> value = encodeBean(obj);

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
    }

    private Map<String, Object> encodeBean(Object obj) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
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

    @Override
    public Object deSerialize(ByteBuf buf) throws Exception {
        return readObject(buf);
    }

    private Object readObject(ByteBuf in) throws Exception {
        byte type = in.readByte();
        switch (type) {
            case TYPE_NULL:
                return null;
            case TYPE_BYTE:
                return in.readByte();
            case TYPE_INTEGER:
                return in.readInt();
            case TYPE_BOOLEAN:
                return in.readBoolean();
            case TYPE_LONG:
                return in.readLong();
            case TYPE_DOUBLE:
                return in.readDouble();
            case TYPE_FLOAT:
                return in.readFloat();
            case TYPE_STRING:
                byte[] stringValue = new byte[in.readInt()];
                in.readBytes(stringValue);
                return new String(stringValue, charset);
            case TYPE_DATE:
                return new Date(in.readLong());
            case TYPE_MAP:
                Map<Object, Object> mapValue = new HashMap<>();
                for (int i = 0, size = in.readInt(); i < size; i++) {
                    mapValue.put(readObject(in), readObject(in));
                }
                if (mapValue.containsKey($_CLASS_NAME_$)) {
                    String className = (String) mapValue.get($_CLASS_NAME_$);
                    return toObject(className, (Map)mapValue);
                }
                return mapValue;
            case TYPE_LIST:
            case TYPE_COLLECTION:
                List<Object> listValue = new ArrayList<>();
                for (int i = 0, size = in.readInt(); i < size; i++) {
                    listValue.add(readObject(in));
                }
                return listValue;
            case TYPE_SET:
                Set<Object> setValue = new LinkedHashSet<>();
                for (int i = 0, size = in.readInt(); i < size; i++) {
                    setValue.add(readObject(in));
                }
                return setValue;
            case TYPE_BYTE_ARRAY:
                int len = in.readInt();
                if (len < 0) {
                    System.out.println(len);
                }
                byte[] byteArray = new byte[len];
                in.readBytes(byteArray);
                return byteArray;
            case TYPE_OBJECT_ARRAY:
                Object[] objectArray = new Object[in.readInt()];
                for (int i = 0, size = objectArray.length; i < size; i++) {
                    objectArray[i] = readObject(in);
                }
                return objectArray;
            case TYPE_CHUNKEDFILE:
                long length = in.readLong();
                ChunkFile chunkFile = createChunkFile(length);
                addDeSerChunkFile(chunkFile);
                return chunkFile;
            case TYPE_CUSTOM:
                String simpleName = (String) readObject(in);
                String className = (String) readObject(in);
                @SuppressWarnings("unchecked")
                Map<String, Object> value = (Map<String, Object>) readObject(in);
                return toObject(className, value);
            default:
                throw new Exception("Not support :" + type);
        }
    }

    private Object toObject(String className, Map<String, Object> map) throws Exception {
        Class<?> clazz = getClass(className);
        if (clazz != null) {
            Object obj = clazz.newInstance();
            // for (Entry<String, Object> entry : value.entrySet()) {
            // BeanUtils.setProperty(obj, entry.getKey().toString(), entry.getValue());
            // }
            decodeBean(obj, map);
            return obj;
        } else {
            if (map != null) {
                map.put($_CLASS_NAME_$, className);
            }
            return map;
        }
    }

    private Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private void decodeBean(Object obj, Map<String, Object> value) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor property : propertyDescriptors) {
            String key = property.getName();
            if (!key.equals("class") && value.containsKey(key)) {
                // 得到property对应的getter方法
                Method setter = property.getWriteMethod();
                setter.invoke(obj, value.get(key));
            }
        }
    }
}
