package com.jssh.netty.support;

import com.jssh.netty.serial.MessageSerial;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class NettyMessageDecoder extends ByteToMessageDecoder {

    private final MessageSerial serial;

    public NettyMessageDecoder(MessageSerial serial) {
        this.serial = serial;
    }

    @Override
    protected synchronized void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Object object = serial.deSerialize(in);
        if (object != null) {
            out.add(object);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        super.exceptionCaught(ctx, cause);
    }
}
