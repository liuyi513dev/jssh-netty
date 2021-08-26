package jssh.netty.rpc.core.server;

import jssh.netty.rpc.core.NettyManager;
import jssh.netty.rpc.core.exception.NettyException;
import jssh.netty.rpc.core.listener.MessageListener;
import jssh.netty.rpc.core.request.HeaderList;
import jssh.netty.rpc.core.request.NettyRequest;
import jssh.netty.rpc.core.request.RequestBuilder;

public interface ServerMessageExchange extends NettyManager {

    default void sendMessage(ClientInfo<?> clientInfo, NettyRequest request, boolean reSendOnNetWorkException,
                             MessageListener listener) {
        request.setClient(clientInfo);
        sendMessage(request, reSendOnNetWorkException, listener);
    }

    default void sendMessage(ClientInfo<?> clientInfo, NettyRequest request, MessageListener listener) {
        sendMessage(clientInfo, request, false, listener);
    }

    default void sendMessage(ClientInfo<?> clientInfo, String requestAction, boolean syn, HeaderList headers,
                             Object body, boolean reSendOnNetWorkException, boolean isAck, MessageListener listener) {
        sendMessage(clientInfo, RequestBuilder.builder().setSyn(syn).setAck(isAck).setRequestAction(requestAction)
                .setHeaders(headers).setBody(body).build(), reSendOnNetWorkException, listener);
    }

    default <T> T sendMessageForResult(ClientInfo<?> clientInfo, String requestAction, HeaderList headers,
                                       Object body) throws NettyException {
        return sendMessageForResult(clientInfo, RequestBuilder.builder().setSyn(true).setRequestAction(requestAction)
                .setHeaders(headers).setBody(body).build());
    }

    @SuppressWarnings("unchecked")
    default <T> T sendMessageForResult(ClientInfo<?> clientInfo, NettyRequest request) throws NettyException {
        Object[] r = new Object[2];
        if (!request.getSyn()) {
            throw new IllegalStateException("request syn must be true");
        }
        sendMessage(clientInfo, request, new MessageListener() {
            @Override
            public void onResponse(Object response) {
                r[1] = response;
            }

            @Override
            public void onException(Throwable e) {
                r[0] = e;
            }
        });
        if (r[0] != null) {
            throw (r[0] instanceof NettyException ? (NettyException) r[0] : new NettyException((Throwable) r[0]));
        }
        return (T) r[1];
    }
}
