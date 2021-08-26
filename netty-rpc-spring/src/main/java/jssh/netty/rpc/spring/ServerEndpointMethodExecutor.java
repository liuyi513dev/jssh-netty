package jssh.netty.rpc.spring;

import jssh.netty.rpc.core.listener.MessageListener;
import jssh.netty.rpc.core.request.NettyRequest;
import jssh.netty.rpc.core.server.ClientInfo;
import jssh.netty.rpc.core.server.Server;
import jssh.netty.rpc.core.server.SimpleClientInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServerEndpointMethodExecutor {

    private Integer listenerIndex;

    private Integer requestIndex;

    private final boolean isSyn;

    private final boolean isReSend;

    private final boolean isAck;

    private final boolean isResponse;

    private final String actionName;

    private final Server server;

    private final List<ArgParam> bodyArgs;

    public ServerEndpointMethodExecutor(Method method, Server server) {
        this.server = server;

        List<ArgParam> args = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        for (int i = 1; i < parameters.length; i++) {
            Parameter p = parameters[i];
            if (NettyRequest.class.isAssignableFrom(p.getType())) {
                this.requestIndex = i;
            } else if (MessageListener.class.isAssignableFrom(p.getType())) {
                this.listenerIndex = i;
            } else {
                args.add(new ArgParam(i, p.getName()));
            }
        }

        Syn sync = method.getAnnotation(Syn.class);
        this.isSyn = sync != null && sync.value();
        ReSend reSend = method.getAnnotation(ReSend.class);
        this.isReSend = reSend != null && reSend.value();
        Ack ack = method.getAnnotation(Ack.class);
        this.isAck = ack != null && ack.value();

        this.isResponse = method.getReturnType() != void.class;
        this.actionName = method.getName();

        this.bodyArgs = args;
    }

    public Object invoke(Object[] args) throws Throwable {
        MessageListener listener = this.listenerIndex != null ? (MessageListener) args[this.listenerIndex] : null;

        ClientInfo<?> clientInfo = args[0] instanceof ClientInfo<?> ? (ClientInfo<?>) args[0]
                : new SimpleClientInfo<>(args[0]);
        if (this.requestIndex != null) {
            NettyRequest request = (NettyRequest) args[this.requestIndex];

            if (this.isResponse) {
                return server.sendMessageForResult(clientInfo, request);
            }
            server.sendMessage(clientInfo, request, isReSend, listener);
            return null;
        }
        Object body = null;
        if (bodyArgs.size() > 0) {
            body = bodyArgs.stream().collect(Collectors.toMap(ArgParam::getName, argParam -> args[argParam.getIndex()]));
        }

        if (isResponse) {
            return server.sendMessageForResult(clientInfo, actionName, null, body);
        }

        server.sendMessage(clientInfo, actionName, isSyn, null, body, isReSend, isAck,
                listener);
        return null;
    }

    private static class ArgParam {
        private int index;
        private String name;

        public ArgParam(int index, String name) {
            this.index = index;
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
