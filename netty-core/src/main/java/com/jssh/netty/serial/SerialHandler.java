package com.jssh.netty.serial;

import io.netty.buffer.ByteBuf;

public interface SerialHandler {
    int getType();

    InnerSerial getInnerSerial();

    interface InnerSerial {
        void write(ByteBuf buf, Object obj) throws Exception;

        Object read(ByteBuf buf) throws Exception;
    }
}
