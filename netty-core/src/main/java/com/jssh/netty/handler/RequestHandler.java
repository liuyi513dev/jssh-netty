package com.jssh.netty.handler;

import com.jssh.netty.request.NettyRequest;
import io.netty.channel.ChannelHandlerContext;

import java.lang.reflect.Method;
import java.util.Map;

public interface RequestHandler {

    void addAction(Map<String, ActionInvocation> actionMapping);

    void addAction(String action, Method method, Object target);

    ReturnValue handleRequest(ChannelHandlerContext ctx, NettyRequest request) throws Exception;
}
