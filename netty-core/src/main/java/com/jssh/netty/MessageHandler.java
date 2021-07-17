package com.jssh.netty;

import com.jssh.netty.request.NettyRequest;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;

@ChannelHandler.Sharable
public class MessageHandler extends SimpleChannelInboundHandler<NettyRequest> {

	private NettyManager nettyManager;

	public MessageHandler(NettyManager nettyManager) {
		this.nettyManager = nettyManager;
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, NettyRequest request) throws Exception {
		nettyManager.receiveRequest(ctx, request);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		nettyManager.active(ctx);
		super.channelActive(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		nettyManager.exception(ctx, cause);
		super.exceptionCaught(ctx, cause);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		nettyManager.inactive(ctx);
		super.channelInactive(ctx);
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			nettyManager.idleStateEvent(ctx, ((IdleStateEvent) evt).state());
		}
	}
}
