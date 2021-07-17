package com.jssh.netty.filter;

import java.util.List;

import com.jssh.netty.request.NettyRequest;

import io.netty.channel.ChannelHandlerContext;

public class NettyRequestChains implements NettyRequestChain {

	private List<NettyRequestFilter> filters;
	private int index;

	public NettyRequestChains(List<NettyRequestFilter> filters) {
		this.filters = filters;
		this.index = 0;
	}

	@Override
	public void doFilter(ChannelHandlerContext ctx, NettyRequest request) {
		if (index < filters.size()) {
			filters.get(index++).doFilter(this, ctx, request);
		}
	}
}
