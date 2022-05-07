package jssh.netty.rpc.core.client;

import jssh.netty.rpc.core.NettyManager;
import jssh.netty.rpc.core.exception.NettyException;
import jssh.netty.rpc.core.listener.MessageListener;
import jssh.netty.rpc.core.request.HeaderList;
import jssh.netty.rpc.core.request.NettyRequest;
import jssh.netty.rpc.core.request.RequestBuilder;

import java.util.concurrent.atomic.AtomicReference;

public interface ClientMessageExchange extends NettyManager {

    default void sendMessage(NettyRequest request, MessageListener listener) {
        sendMessage(request, false, listener);
    }

    default void sendMessage(String requestAction, boolean syn, HeaderList headers, Object body,
                             boolean reSendOnNetWorkException, boolean ack, MessageListener listener) {
        sendMessage(RequestBuilder.builder().setSyn(syn).setAck(ack).setRequestAction(requestAction)
                .setHeaders(headers).setBody(body).build(), reSendOnNetWorkException, listener);
    }

    default <T> T sendMessageForResult(String requestAction, HeaderList headers, Object body)
            throws NettyException {
        return sendMessageForResult(RequestBuilder.builder().setSyn(true).setRequestAction(requestAction)
                .setHeaders(headers).setBody(body).build());
    }

    @SuppressWarnings("unchecked")
    default <T> T sendMessageForResult(NettyRequest request) throws NettyException {
        AtomicReference<Object> responseRef = new AtomicReference<>();
        AtomicReference<Throwable> expRef = new AtomicReference<>();
        if (!request.getSyn()) {
            throw new IllegalStateException("request syn must be true");
        }
        sendMessage(request, new MessageListener() {
            @Override
            public void onResponse(Object response) {
                responseRef.set(response);
            }

            @Override
            public void onException(Throwable e) {
                expRef.set(e);
            }
        });
        if (expRef.get() != null) {
            throw (expRef.get() instanceof NettyException ? (NettyException) expRef.get() : new NettyException(expRef.get()));
        }
        return (T) responseRef.get();
    }
}
