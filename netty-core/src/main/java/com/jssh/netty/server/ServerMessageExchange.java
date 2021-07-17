package com.jssh.netty.server;

import com.jssh.netty.NettyManager;
import com.jssh.netty.exception.NettyException;
import com.jssh.netty.listener.MessageListener;
import com.jssh.netty.request.HeaderList;
import com.jssh.netty.request.NettyRequest;
import com.jssh.netty.request.RequestBuilder;

public interface ServerMessageExchange extends NettyManager {

    default void sendMessage(ClientInfo<?> clientInfo, NettyRequest request, boolean reSendOnNetWorkException,
                             MessageListener listener) {
        request.setClientInfo(clientInfo);
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
            throw new NettyException("request syn must be true");
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
            throw (r[0] instanceof NettyException ? (NettyException) r[0] : new NettyException((Exception) r[0]));
        }
        return (T) r[1];
    }
}
