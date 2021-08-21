package com.jssh.netty.support;

import com.jssh.netty.request.BufNettyRequest;
import com.jssh.netty.request.NettyRequest;
import com.jssh.netty.serial.BodyBuf;
import com.jssh.netty.serial.ChunkFile;
import com.jssh.netty.serial.DefaultSerial;
import com.jssh.netty.serial.FileMessageSerial;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.stream.ChunkedFile;

import java.util.List;

public class NettyMessageEncoder extends MessageToMessageEncoder<NettyRequest> {

    private final boolean isBodyBuf;

    private final FileMessageSerial serial;

    public NettyMessageEncoder() {
        this(new DefaultSerial());
    }

    public NettyMessageEncoder(FileMessageSerial serial) {
        this(true, serial);
    }

    public NettyMessageEncoder(boolean isBodyBuf, FileMessageSerial serial) {
        this.serial = serial;
        this.isBodyBuf = isBodyBuf;
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
        serial.serialize(buf, request.getRequired());
        serial.serialize(buf, request.getRequestAction());
        serial.serialize(buf, request.getRequestId());
        serial.serialize(buf, request.getResponseId());
        serial.serialize(buf, request.getHeaders());

        if (isBodyBuf && request instanceof BufNettyRequest) {

            BodyBuf bodyBuf = ((BufNettyRequest) request).getBodyBuf();
            synchronized (bodyBuf) {
                ByteBuf body = bodyBuf.getBodyBuf();
                body.markReaderIndex();
                buf.writeBytes(body, bodyBuf.getStart(), bodyBuf.getLength());
                body.resetReaderIndex();
            }

            out.add(buf);

            serial.writeSerChunkFiles(out);

            List<ChunkFile> bodyBufFiles = bodyBuf.getBodyBufFiles();

            if (bodyBufFiles != null && bodyBufFiles.size() > 0) {
                for (ChunkFile f : bodyBufFiles) {
                    out.add(new ChunkedFile(f.getSrcFile()));
                }
            }

        } else {
            serial.serialize(buf, request.getBody());
            out.add(buf);
            serial.writeSerChunkFiles(out);
        }
    }
}
