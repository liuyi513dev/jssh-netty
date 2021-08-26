package jssh.netty.rpc.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import jssh.netty.rpc.core.listener.MessageListener;
import jssh.netty.rpc.core.request.NettyRequest;

public interface NettyManager {

    void init() throws Exception;

    void start() throws Exception;

    void close() throws Exception;

    void receiveRequest(ChannelHandlerContext ctx, NettyRequest request) throws Exception;

    void sendMessage(NettyRequest request, boolean reSendOnNetWorkException, MessageListener listener);

    void active(ChannelHandlerContext ctx) throws Exception;

    void exception(ChannelHandlerContext ctx, Throwable cause) throws Exception;

    void inactive(ChannelHandlerContext ctx) throws Exception;

    void idleStateEvent(ChannelHandlerContext ctx, IdleState idleState) throws Exception;

    Configuration getConfiguration();
}
