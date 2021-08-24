package com.jssh.netty.support;

import com.jssh.netty.request.NettyRequest;
import com.jssh.netty.serial.MessageSerial;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class NettyMessageEncoder extends MessageToMessageEncoder<NettyRequest> {

    private final MessageSerial serial;

    public NettyMessageEncoder(MessageSerial serial) {
        this.serial = serial;
    }

    @Override
    protected synchronized void encode(ChannelHandlerContext ctx, NettyRequest request, List<Object> out)
            throws Exception {
        if (request == null) {
            throw new Exception("The encode message is null");
        }
        ByteBuf buf = ctx.alloc().heapBuffer();
        out.add(buf);
        serial.serialize(buf, request, out);
    }
}
