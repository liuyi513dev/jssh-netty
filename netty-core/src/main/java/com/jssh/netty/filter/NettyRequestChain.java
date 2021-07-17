package com.jssh.netty.filter;

import com.jssh.netty.request.NettyRequest;

import io.netty.channel.ChannelHandlerContext;

public interface NettyRequestChain {

	void doFilter(ChannelHandlerContext ctx, NettyRequest request);
}
