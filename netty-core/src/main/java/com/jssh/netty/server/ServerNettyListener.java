package com.jssh.netty.server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

public interface ServerNettyListener {

	default void onServerStart(ChannelFuture serverChannelFuture) {

	}

	default void onServerStop(ChannelFuture serverChannelFuture) {

	}

	default void onChannelDisConnected(ChannelHandlerContext ctx, ClientInfo<?> client) {

	}

	default void onChannelConnected(ChannelHandlerContext ctx, ClientInfo<?> client) {

	}

}
