package com.jssh.netty.client;

import io.netty.channel.ChannelHandlerContext;

public interface ClientInfoProvider {

	Object clientInfo(ChannelHandlerContext ctx);
}
