package com.jssh.netty;

import com.jssh.netty.listener.MessageListener;
import com.jssh.netty.request.NettyRequest;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;

public interface NettyManager {

	void start() throws Exception;

	void stop() throws Exception;

	void receiveRequest(ChannelHandlerContext ctx, NettyRequest request) throws Exception;

	void sendMessage(NettyRequest request, boolean reSendOnNetWorkException, MessageListener listener);

	void active(ChannelHandlerContext ctx) throws Exception;

	void exception(ChannelHandlerContext ctx, Throwable cause) throws Exception;

	void inactive(ChannelHandlerContext ctx) throws Exception;

	void idleStateEvent(ChannelHandlerContext ctx, IdleState idleState) throws Exception;
}
