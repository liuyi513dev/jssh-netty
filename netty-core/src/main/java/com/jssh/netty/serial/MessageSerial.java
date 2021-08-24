package com.jssh.netty.serial;

import io.netty.buffer.ByteBuf;

import java.util.List;

public interface MessageSerial {

    void serialize(ByteBuf buf, Object object) throws Exception;

    default void serialize(ByteBuf buf, Object object, List<Object> out) throws Exception {
        serialize(buf, object);
    }

    Object deSerialize(ByteBuf in) throws Exception;
}
