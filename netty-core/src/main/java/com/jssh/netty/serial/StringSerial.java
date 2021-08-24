package com.jssh.netty.serial;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class StringSerial implements MessageSerial {

    public static final Charset default_charset = StandardCharsets.UTF_8;

    @Override
    public void serialize(ByteBuf buf, Object object) throws Exception {
        Objects.requireNonNull(object);

        String value = parseString(object);
        int lenIndex = buf.writerIndex();
        buf.writeInt(0);
        int len = buf.writeCharSequence(value, default_charset);
        buf.markWriterIndex();
        buf.setInt(lenIndex, len);
        buf.resetWriterIndex();
    }

    @Override
    public Object deSerialize(ByteBuf in) throws Exception {
        String string = in.readCharSequence(in.readInt(), default_charset).toString();
        return parseObject(string);
    }

    public String parseString(Object object) {
        return (String) object;
    }

    public Object parseObject(String string) {
        return string;
    }
}
