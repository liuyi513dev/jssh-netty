package com.jssh.netty.support;

import com.jssh.netty.request.BodyBufRequestBuilder;
import com.jssh.netty.request.HeaderList;
import com.jssh.netty.serial.BodyBuf;
import com.jssh.netty.serial.ChunkFile;
import com.jssh.netty.serial.DefaultSerial;
import com.jssh.netty.serial.FileMessageSerial;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NettyMessageDecoder extends ByteToMessageDecoder {

    private final boolean isBodyBuf;

    private final FileMessageSerial serial;

    private MessageDecoderHolder messageHolder;

    public NettyMessageDecoder() {
        this(new DefaultSerial());
    }

    public NettyMessageDecoder(FileMessageSerial serial) {
        this(true, serial);
    }

    public NettyMessageDecoder(boolean isBodyBuf, FileMessageSerial serial) {
        this.serial = serial;
        this.isBodyBuf = isBodyBuf;
    }

    @Override
    protected synchronized void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if (messageHolder != null) {
            if (messageHolder.writeAndCheckComplete(in)) {
                out.add(messageHolder.getBuilder().build());
                messageHolder = null;
            }
            return;
        }

        BodyBufRequestBuilder builder = BodyBufRequestBuilder.builder();

        builder.setSyn((Boolean) serial.deSerialize(in));
        builder.setAck((Boolean) serial.deSerialize(in));
        builder.setRequired((Boolean) serial.deSerialize(in));
        builder.setRequestAction((String) serial.deSerialize(in));
        builder.setRequestId((String) serial.deSerialize(in));
        builder.setResponseId((String) serial.deSerialize(in));
        builder.setHeaders((HeaderList) serial.deSerialize(in));

        int start = in.readerIndex();
        Object body = serial.deSerialize(in);
        builder.setBody(body);
        int end = in.readerIndex();

        if (isBodyBuf && body != null && end > start) {
            // message.setBodyBuf(in.copy(start, end - start));
            List<ChunkFile> chunkFiles = serial.getDeSerChunkFiles();
            List<ChunkFile> bodyBufFiles = null;
            if (chunkFiles != null && chunkFiles.size() > 0) {
                bodyBufFiles = new ArrayList<>(chunkFiles);
            }
            BodyBuf bodyBuf = new BodyBuf(in, start, end - start, bodyBufFiles);
            builder.setBodyBuf(bodyBuf);
        }

        if (serial.deSerializeComplete(builder, in)) {
            out.add(builder.build());
            return;
        }

        messageHolder = new MessageDecoderHolder(builder);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        super.exceptionCaught(ctx, cause);
    }

    class MessageDecoderHolder {

        private final BodyBufRequestBuilder builder;

        public MessageDecoderHolder(BodyBufRequestBuilder builder) {
            this.builder = builder;
        }

        public BodyBufRequestBuilder getBuilder() {
            return builder;
        }

        public boolean writeAndCheckComplete(ByteBuf in) throws IOException {
            return serial.deSerializeComplete(builder, in);
        }
    }
}
