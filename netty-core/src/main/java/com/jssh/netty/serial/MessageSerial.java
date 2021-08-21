package com.jssh.netty.serial;

import io.netty.buffer.ByteBuf;

public interface MessageSerial {

    void serialize(ByteBuf buf, Object body) throws Exception;

    Object deSerialize(ByteBuf body) throws Exception;
}
