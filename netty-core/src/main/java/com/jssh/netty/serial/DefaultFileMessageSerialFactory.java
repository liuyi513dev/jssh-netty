package com.jssh.netty.serial;

public class DefaultFileMessageSerialFactory implements FileMessageSerialFactory {

    @Override
    public FileMessageSerial createFileMessageSerial() {
        return new DefaultSerial();
    }
}
