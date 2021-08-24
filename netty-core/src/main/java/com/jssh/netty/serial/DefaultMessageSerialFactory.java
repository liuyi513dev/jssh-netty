package com.jssh.netty.serial;

public class DefaultMessageSerialFactory implements MessageSerialFactory {

    @Override
    public MessageSerial createMessageSerial() {
        return new NettyRequestSerial();
    }
}
