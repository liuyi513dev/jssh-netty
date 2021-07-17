package com.jssh.netty.support;

import com.jssh.netty.request.BodyBufRequestBuilder;
import com.jssh.netty.request.HeaderList;
import com.jssh.netty.serial.BodyBuf;
import com.jssh.netty.serial.ChunkFile;
import com.jssh.netty.serial.DefaultSerial;
import com.jssh.netty.serial.MessageSerial;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class NettyMessageDecoder extends ByteToMessageDecoder {

    public static final byte TYPE_NULL = 0;
    public static final byte TYPE_BYTE = 1;
    public static final byte TYPE_INTEGER = 2;
    public static final byte TYPE_BOOLEAN = 3;
    public static final byte TYPE_LONG = 4;
    public static final byte TYPE_DOUBLE = 5;
    public static final byte TYPE_STRING = 6;
    public static final byte TYPE_DATE = 7;
    public static final byte TYPE_MAP = 8;
    public static final byte TYPE_LIST = 9;
    public static final byte TYPE_BYTE_ARRAY = 10;
    public static final byte TYPE_CUSTOM = 11;
    public static final byte TYPE_OBJECT_ARRAY = 12;
    public static final byte TYPE_CHUNKEDFILE = 13;
    public static final byte TYPE_SET = 14;
    public static final byte TYPE_FLOAT = 15;
    public static final byte TYPE_COLLECTION = 16;

    public static final byte TYPE_CUSTOM_SERIAL = 20;

    private MessageSerial serial;
    private MessageSerial bodySerial;
    private boolean isBodyBuf;

    private MessageDecoderHolder messageHolder;

    public NettyMessageDecoder() {
        this(true, null);
    }

    public NettyMessageDecoder(boolean isBodyBuf) {
        this(isBodyBuf, null);
    }

    public NettyMessageDecoder(MessageSerial messageSerial) {
        this(true, messageSerial);
    }

    public NettyMessageDecoder(boolean isBodyBuf, MessageSerial bodySerial) {
        this.serial = new DefaultSerial();
        this.isBodyBuf = isBodyBuf;
        this.bodySerial = bodySerial;
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
        builder.setRequestAction((String) serial.deSerialize(in));
        builder.setRequestId((String) serial.deSerialize(in));
        builder.setResponseId((String) serial.deSerialize(in));
        builder.setHeaders((HeaderList) serial.deSerialize(in));

        int start = in.readerIndex();

        boolean isDefaultSerial = in.readBoolean();
        MessageSerial ser = isDefaultSerial ? serial : bodySerial;
        Object body = ser.deSerialize(in);

        builder.setBody(body);

        int end = in.readerIndex();

        if (isBodyBuf && body != null && end > start) {
            // message.setBodyBuf(in.copy(start, end - start));
            List<ChunkFile> chunkFiles = ser.getDeSerChunkFiles();
            List<ChunkFile> bodyBufFiles = null;
            if (chunkFiles != null && chunkFiles.size() > 0) {
                bodyBufFiles = new ArrayList<>(chunkFiles);
            }
            BodyBuf bodyBuf = new BodyBuf(in, start, end - start, bodyBufFiles);
            builder.setBodyBuf(bodyBuf);
        }

        if (ser.deSerializeComplete(builder, in)) {
            out.add(builder.build());
            return;
        }

        messageHolder = new MessageDecoderHolder(ser, builder);
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        super.exceptionCaught(ctx, cause);
    }

    static class MessageDecoderHolder {

        private MessageSerial ser;

        private BodyBufRequestBuilder builder;

        public MessageDecoderHolder(MessageSerial ser, BodyBufRequestBuilder builder) {
            this.ser = ser;
            this.builder = builder;
        }

        public BodyBufRequestBuilder getBuilder() {
            return builder;
        }

        public boolean writeAndCheckComplete(ByteBuf in) throws IOException {
            return ser.deSerializeComplete(builder, in);
        }
    }
}
