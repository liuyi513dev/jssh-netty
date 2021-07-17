package com.jssh.netty.support;

import com.jssh.netty.request.NettyRequest;
import com.jssh.netty.serial.BodyBuf;
import com.jssh.netty.serial.ChunkFile;
import com.jssh.netty.serial.DefaultSerial;
import com.jssh.netty.serial.MessageSerial;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.stream.ChunkedFile;

import java.util.List;

public class NettyMessageEncoder extends MessageToMessageEncoder<NettyRequest> {

    private boolean isBodyBuf;
    private MessageSerial serial;
    private MessageSerial bodySerial;

    public NettyMessageEncoder() {
        this(true, null);
    }

    public NettyMessageEncoder(MessageSerial bodySerial) {
        this(true, bodySerial);
    }

    public NettyMessageEncoder(boolean isBodyBuf, MessageSerial bodySerial) {
        this.serial = new DefaultSerial();
        this.isBodyBuf = isBodyBuf;
        this.bodySerial = bodySerial;
    }

    @Override
    protected synchronized void encode(ChannelHandlerContext ctx, NettyRequest request, List<Object> out)
            throws Exception {

        if (request == null) {
            throw new Exception("The encode message is null");
        }
        ByteBuf buf = ctx.alloc().heapBuffer();

        serial.serialize(buf, request.getSyn());
        serial.serialize(buf, request.getAck());
        serial.serialize(buf, request.getRequestAction());
        serial.serialize(buf, request.getRequestId());
        serial.serialize(buf, request.getResponseId());
        serial.serialize(buf, request.getHeaders());

        if (isBodyBuf && request.getBodyBuf() != null) {

            BodyBuf bodyBuf = request.getBodyBuf();
            synchronized (bodyBuf) {
                ByteBuf body = bodyBuf.getBodyBuf();
                body.markReaderIndex();
                buf.writeBytes(body, bodyBuf.getStart(), bodyBuf.getLength());
                body.resetReaderIndex();
            }

            out.add(buf);

            serial.afterSerialize(out);

            List<ChunkFile> bodyBufFiles = request.getBodyBuf().getBodyBufFiles();

            if (bodyBufFiles != null && bodyBufFiles.size() > 0) {
                for (ChunkFile f : bodyBufFiles) {
                    out.add(new ChunkedFile(f.getSrcFile()));
                }
            }

        } else {
            Object body = request.getBody();
            if (body != null && bodySerial != null && bodySerial.support(body)) {
                buf.writeBoolean(false);
                bodySerial.serialize(buf, body);
                out.add(buf);
                serial.afterSerialize(out);
                bodySerial.afterSerialize(out);
            } else {
                buf.writeBoolean(true);
                serial.serialize(buf, body);
                out.add(buf);
                serial.afterSerialize(out);
            }
        }
    }
}
