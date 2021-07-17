package com.jssh.netty.server;

import io.netty.channel.ChannelHandlerContext;

public class ClientChannel {

	private ChannelHandlerContext ctx;
	
	private ClientInfo<?> clientInfo;
	
	public ClientChannel(ChannelHandlerContext ctx, ClientInfo<?> clientInfo) {
		super();
		this.ctx = ctx;
		this.clientInfo = clientInfo;
	}

	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	public ClientInfo<?> getClientInfo() {
		return clientInfo;
	}
	
	@Override
	public String toString() {
		return "ClientChannel [" + (ctx != null ? "ctx=" + ctx + ", " : "")
				+ (clientInfo != null ? "clientInfo=" + clientInfo : "") + "]";
	}
}
