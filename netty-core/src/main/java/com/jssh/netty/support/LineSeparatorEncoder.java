package com.jssh.netty.support;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class LineSeparatorEncoder extends StringEncoder {

    private static final String SEPARATOR = System.getProperty("line.separator");

    public LineSeparatorEncoder() {
        super(StandardCharsets.UTF_8);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CharSequence msg, List<Object> out) throws Exception {
        super.encode(ctx, msg, out);
        super.encode(ctx, SEPARATOR, out);
    }
}
