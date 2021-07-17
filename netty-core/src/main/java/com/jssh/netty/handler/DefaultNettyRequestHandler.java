package com.jssh.netty.handler;

import com.jssh.netty.exception.ActionNotFoundException;
import com.jssh.netty.request.NettyRequest;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultNettyRequestHandler implements RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultNettyRequestHandler.class);

    private final Map<String, ActionInvocation> actions = new ConcurrentHashMap<>(20);

    @Override
    public void addAction(Map<String, ActionInvocation> actionMapping) {
        actions.putAll(actionMapping);
        for (Map.Entry<String, ActionInvocation> entry : actionMapping.entrySet()) {
            logger.info("Action mapping [{}] -> {}", entry.getKey(), entry.getValue().getMethod());
        }
    }

    @Override
    public void addAction(String action, Method method, Object target) {
        actions.put(action, new ActionInvocation(method, target));
        logger.info("Action mapping [{}] -> {}", action, method);
    }

    @Override
    public ReturnValue handleRequest(ChannelHandlerContext ctx, NettyRequest request) throws Exception {
        ActionInvocation methodInvocation = actions.get(request.getRequestAction());
        if (methodInvocation != null) {
            return methodInvocation.invoke(request);
        } else {
            throw new ActionNotFoundException(request.getRequestAction());
        }
    }
}
