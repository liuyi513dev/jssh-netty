package com.jssh.netty.support;

import java.nio.charset.Charset;
import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.string.StringEncoder;

public class LineSeparatorEncoder extends StringEncoder {

	private static final String SEPARATOR = System.getProperty("line.separator");

	public LineSeparatorEncoder() {
		super(Charset.forName("UTF-8"));
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, CharSequence msg, List<Object> out) throws Exception {
		super.encode(ctx, msg, out);
		super.encode(ctx, SEPARATOR, out);
	}
}
