package com.jssh.netty.client;

import io.netty.channel.ChannelHandlerContext;

public interface ClientNettyListener {
	
	default void onChannelDisconnected(ChannelHandlerContext ctx) {

	}

	default void onChannelConnected(ChannelHandlerContext ctx) {

	}
}
