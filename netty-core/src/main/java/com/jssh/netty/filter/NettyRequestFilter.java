package com.jssh.netty.filter;

import com.jssh.netty.request.NettyRequest;

import io.netty.channel.ChannelHandlerContext;

public interface NettyRequestFilter {

	void doFilter(NettyRequestChain chain, ChannelHandlerContext ctx, NettyRequest request);
}
