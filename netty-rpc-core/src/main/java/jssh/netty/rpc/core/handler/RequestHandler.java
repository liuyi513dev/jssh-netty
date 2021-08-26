package jssh.netty.rpc.core.handler;

import io.netty.channel.ChannelHandlerContext;
import jssh.netty.rpc.core.request.NettyRequest;

import java.lang.reflect.Method;
import java.util.Map;

public interface RequestHandler {

    void addAction(Map<String, ActionInvocation> actionMapping);

    void addAction(String action, Method method, Object target);

    ReturnValue handleRequest(ChannelHandlerContext ctx, NettyRequest request) throws Exception;
}
