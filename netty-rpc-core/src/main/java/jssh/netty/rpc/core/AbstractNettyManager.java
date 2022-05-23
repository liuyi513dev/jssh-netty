package jssh.netty.rpc.core;

import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.GenericFutureListener;
import jssh.netty.rpc.core.exception.NettyException;
import jssh.netty.rpc.core.exception.NetworkException;
import jssh.netty.rpc.core.exception.TimeoutException;
import jssh.netty.rpc.core.filter.NettyRequestChains;
import jssh.netty.rpc.core.filter.NettyRequestFilter;
import jssh.netty.rpc.core.handler.DefaultNettyRequestHandler;
import jssh.netty.rpc.core.handler.RequestHandler;
import jssh.netty.rpc.core.handler.ReturnValue;
import jssh.netty.rpc.core.listener.MessageListener;
import jssh.netty.rpc.core.listener.NettyResponse;
import jssh.netty.rpc.core.request.*;
import jssh.netty.rpc.core.serial.DefaultMessageSerialFactory;
import jssh.netty.rpc.core.serial.ErrorObject;
import jssh.netty.rpc.core.serial.MessageSerial;
import jssh.netty.rpc.core.serial.MessageSerialFactory;
import jssh.netty.rpc.core.ssl.SSLHandler;
import jssh.netty.rpc.core.support.NettyMessageDecoder;
import jssh.netty.rpc.core.support.NettyMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

//import io.netty.util.ResourceLeakDetector;

public abstract class AbstractNettyManager implements NettyManager, Closeable {

    protected static final String PING_REQUEST = "PING";
    protected static final String PONG_REQUEST = "PONG";
    protected static final String CONNECT_REQUEST = "CONNECT";
    protected static final String RESPONSE_REQUEST = "RESPONSE";
    protected static final String ACK_REQUEST = "ACK";
    protected static final String ERROR_REQUEST = "ERROR";

    private static final Logger logger = LoggerFactory.getLogger(AbstractNettyManager.class);

    static {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    }

    private Configuration configuration;

    private final ConcurrentMap<String, NettyResponse> responseMap = new ConcurrentHashMap<>();

    private final List<NettyRequestFilter> dataHandleFilters = new CopyOnWriteArrayList<>();

    protected ScheduledExecutorService executor;

    private ExecutorService receiveWorker;
    private ExecutorService sendWorker;

    private RequestHandler requestHandler = new DefaultNettyRequestHandler();

    private final CopyOnWriteArrayList<RequestContext> sendListOnConnected = new CopyOnWriteArrayList<>();

    private final MessageHandler handler = new MessageHandler(this);

    private SSLHandler ssl = new SSLHandler();

    private RequestId requestId = new DefaultRequestId();

    private MessageSerialFactory messageSerialFactory = new DefaultMessageSerialFactory();

    public void initExecutors() {
        this.executor = Executors.newScheduledThreadPool(getConfiguration().getExecutorSize());
        this.executor.scheduleWithFixedDelay(new SendListOnConnectedTask(), 30000, 30000, TimeUnit.MILLISECONDS);
        this.receiveWorker = Executors.newCachedThreadPool();
        this.sendWorker = Executors.newCachedThreadPool();
    }

    @Override
    public void init() throws Exception {
        initExecutors();
        initRequestFilter();
        initSSL();
        initBootstrap();
    }

    @Override
    public void start() {
        initConnection();
    }

    @Override
    public void close() {
        logger.info("shutdown netty");
        this.executor.shutdownNow();
        this.receiveWorker.shutdownNow();
        this.sendWorker.shutdownNow();
        this.responseMap.clear();
        this.stopConnection();
    }

    protected void initRequestFilter() {

    }

    protected void initSSL() throws Exception {
        ssl.initSSL(getConfiguration().getSsl());
    }

    protected void initBootstrap() {

    }

    protected void initSocketChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        // 处理日志
        if (getConfiguration().isLogging()) {
            pipeline.addLast(new LoggingHandler(LogLevel.INFO));
        }
        //TLS
        SSLEngine sslEngine = createSSLEngine();
        if (sslEngine != null) {
            pipeline.addLast("ssl", new SslHandler(sslEngine));
        }
        // 处理心跳
        pipeline.addLast(
                new IdleStateHandler(getConfiguration().getReaderIdleMillis(), getConfiguration().getWriterIdleMillis(), getConfiguration().getAllIdleMillis(), TimeUnit.MILLISECONDS));
        //处理粘包
        pipeline.addLast(new LengthFieldPrepender(4, false));
        pipeline.addLast(new LengthFieldBasedFrameDecoder(getConfiguration().getMaxFrameLength(), 0, 4, 0, 4));
        //处理文件传输
        pipeline.addLast("streamer", new ChunkedWriteHandler());
        //编解码器
        MessageSerial messageSerial = messageSerialFactory.createMessageSerial();
        pipeline.addLast("decoder", new NettyMessageDecoder(messageSerial));
        pipeline.addLast("encoder", new NettyMessageEncoder(messageSerial));
        pipeline.addLast("writeTO", new WriteTimeoutHandler(getConfiguration().getWriteTimeoutMillis(), TimeUnit.MILLISECONDS));
        pipeline.addLast("handler", handler);
    }

    protected SSLEngine createSSLEngine() {
        return ssl.createSSLEngine();
    }

    protected void initConnection() {
    }

    protected void stopConnection() {

    }

    public void addNettyRequestFilter(NettyRequestFilter filter) {
        dataHandleFilters.add(filter);
    }

    @Override
    public void receiveRequest(ChannelHandlerContext ctx, NettyRequest message) {
        receiveWorker.submit(() -> {
            try {
                doReceiveMessage(ctx, message);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    public void doReceiveMessage(ChannelHandlerContext ctx, NettyRequest request) throws Exception {
        if (dataHandleFilters.size() > 0) {
            NettyRequestChains chain = new NettyRequestChains(dataHandleFilters);
            chain.doFilter(ctx, request);
        }

        if (getConfiguration().isPrintMessage()) {
            logger.info("<<--- receiving message [{}] {}", ctx.channel().attr(Attributes.CHANNEL_NAME).get(), request);
        }

        String responseId = request.getResponseId();
        if (responseId != null) {
            doResponseHandle(responseId, request);
            return;
        }

        String requestId = request.getRequestId();
        if (requestId != null && request.getAck()) {
            NettyRequest ackRequest = RequestBuilder.builder().setResponseId(requestId)
                    .setRequestAction(ACK_REQUEST).build();
            ackRequest.setClient(request.getClient());
            sendMessage(ctx.channel(), ackRequest, null);
        }

        doMessageHandle(ctx, request);
    }

    protected void doResponseHandle(String responseId, NettyRequest request) throws Exception {
        NettyResponse nettyResult = responseMap.get(responseId);
        if (nettyResult != null) {
            switch (request.getRequestAction()) {
                case ACK_REQUEST:
                    nettyResult.setAck();
                    break;
                case ERROR_REQUEST:
                    nettyResult.setError((ErrorObject) request.getBody());
                    responseMap.remove(responseId);
                    break;
                case RESPONSE_REQUEST:
                    nettyResult.setResult(request.getBody());
                    responseMap.remove(responseId);
                    break;
                default:
            }
        }
    }

    protected void doMessageHandle(ChannelHandlerContext ctx, NettyRequest request) throws Exception {
        switch (request.getRequestAction()) {
            case PING_REQUEST:
                doPingMessage(ctx);
                break;
            case PONG_REQUEST:
                doPongMessage(ctx);
                break;
            case CONNECT_REQUEST:
                doConnectionRequest(ctx, request);
                break;
            default:
                doActionRequest(ctx, request);
        }
    }

    public void doPingMessage(ChannelHandlerContext ctx) {

    }

    public void doPongMessage(ChannelHandlerContext ctx) {

    }

    public void doConnectionRequest(ChannelHandlerContext ctx, NettyRequest request) {

    }

    public void doActionRequest(ChannelHandlerContext ctx, NettyRequest request) {
        ReturnValue responseValue = null;
        NettyRequest response = null;
        try {
            responseValue = requestHandler.handleRequest(ctx, request);
        } catch (Exception e) {
            Throwable throwable = e;
            while (throwable instanceof InvocationTargetException && throwable.getCause() != null) {
                throwable = throwable.getCause();
            }

            logger.error(e.getMessage(), throwable);
            response = RequestBuilder.builder().setResponseId(request.getRequestId())
                    .setRequestAction(ERROR_REQUEST).setBody(new ErrorObject(throwable)).build();
        }

        if (responseValue != null && responseValue.isReturn()) {
            response = RequestBuilder.builder().setResponseId(request.getRequestId())
                    .setRequestAction(RESPONSE_REQUEST).setBody(responseValue.getReturnValue()).build();
        }

        if (response == null && request.getRequired()) {
            response = RequestBuilder.builder().setResponseId(request.getRequestId())
                    .setRequestAction(RESPONSE_REQUEST).setBody(null).build();
        }

        if (response != null) {
            response.setClient(request.getClient());
            sendMessage(response, null);
        }
    }

    public void onConnectionSuccess(ChannelHandlerContext ctx) {

    }

    @Override
    public void inactive(ChannelHandlerContext ctx) {
        for (NettyResponse response : responseMap.values()) {
            if (Objects.equals(response.getChannelId(), ctx.channel().id().asLongText())) {
                response.setException(new NetworkException("netty connection inactive"));
            }
        }
    }

    @Override
    public void idleStateEvent(ChannelHandlerContext ctx, IdleState idleState) throws Exception {
        switch (idleState) {
            case WRITER_IDLE:
                logger.info("---> PING {}", ctx.channel().attr(Attributes.CHANNEL_NAME).get());
                ctx.channel().writeAndFlush(
                        RequestBuilder.builder().setRequestAction(PING_REQUEST).build());
                break;
            case READER_IDLE:
            case ALL_IDLE:
                logger.info("close {} {}", ctx, idleState);
                ctx.close();
                break;
        }
    }

    @Override
    public void exception(ChannelHandlerContext ctx, Throwable cause) {
        logger.error(cause.getMessage(), cause);
        logger.info("close {}", ctx);
        ctx.close();
    }

    public void sendMessage(NettyRequest request, MessageListener listener) {
        sendMessage(null, request, listener);
    }

    public void sendMessage(Channel bindChannel, NettyRequest request, MessageListener listener) {
        sendMessage(bindChannel, request, false, listener);
    }

    @Override
    public void sendMessage(NettyRequest request, boolean reSendOnNetWorkException, MessageListener listener) {
        sendMessage(null, request, reSendOnNetWorkException, listener);
    }

    public void sendMessage(Channel bindChannel, NettyRequest request, boolean reSendOnNetWorkException, MessageListener listener) {
        sendRequest(new RequestContext(request, reSendOnNetWorkException, bindChannel, listener));
    }

    private void sendRequest(RequestContext requestContext) {

        NettyRequest request = requestContext.getRequest();

        if (request.getRequestId() == null) {
            request.setRequestId(requestId.nextId());
        }

        if (request.getSyn()) {

            if (requestContext.isReSendOnNetWorkException() || request.getAck()) {

                AtomicReference<Throwable> exp = new AtomicReference<>();
                requestContext.getListeners().add(new MessageListener() {
                    @Override
                    public void onException(Throwable e) {
                        exp.set(e);
                    }
                });

                int retry = 0;
                do {
                    if (retry != 0) {
                        try {
                            Thread.sleep(5 * 1000);
                        } catch (InterruptedException e1) {

                        }
                    }
                    retry++;
                    exp.set(null);

                    Channel bindChannel = requestContext.getBindChannel();

                    sendRequest(bindChannel != null ? bindChannel : getChannel(request), requestContext.getRequest(),
                            requestContext.getListeners());

                } while ((requestContext.isReSendOnNetWorkException() && exp.get() instanceof NetworkException
                        || request.getAck() && exp.get() instanceof TimeoutException) && retry < 10);

                if (exp.get() != null) {
                    throw new NettyException(exp.get());
                }
            } else {

                Channel bindChannel = requestContext.getBindChannel();

                sendRequest(bindChannel != null ? bindChannel : getChannel(request), requestContext.getRequest(),
                        requestContext.getListeners());
            }

        } else {

            if (requestContext.isReSendOnNetWorkException() || request.getAck()) {
                requestContext.getListeners().add(new MessageListener() {
                    @Override
                    public void onException(Throwable e) {
                        logger.error(request.getRequestId() + " " + e.getMessage(), e);
                        if (requestContext.isReSendOnNetWorkException() && e instanceof NetworkException
                                || request.getAck() && e instanceof TimeoutException) {
                            addDelayList(requestContext, e);
                            requestContext.getListeners().remove(this);
                        }
                    }
                });
            }

            Channel bindChannel = requestContext.getBindChannel();
            sendRequest(bindChannel != null ? bindChannel : getChannel(request), requestContext.getRequest(),
                    requestContext.getListeners());
        }
    }

    private void sendRequest(Channel channel, NettyRequest request, List<MessageListener> listeners) {

        MessageListenerExecutor listenerExe = new MessageListenerExecutor(listeners);
        if (channel == null || !channel.isActive()) {
            listenerExe.fireException(new NetworkException("Channel is null or not active."));
            return;
        }

        String requestId = request.getRequestId();

        if (getConfiguration().isPrintMessage()) {
            logger.info("---> send message [{}] {}", channel.attr(Attributes.CHANNEL_NAME).get(), request);
        }

        boolean syn = request.getSyn();
        boolean ack = request.getAck();
        boolean required = listenerExe.hasResponseListener();

        request.setRequired(required);

        if ((ack || required) && requestId == null) {
            throw new IllegalStateException("requestId can't be null");
        }

        final NettyResponse response = (ack || required) ? new NettyResponse(requestId, ack, required, channel.id().asLongText())
                : null;

        if (response != null) {
            responseMap.put(requestId, response);
        }

        ChannelFuture channelFuture;

        if (listenerExe.hasProgressListener()) {
            ChannelProgressivePromise progressivePromise = channel.newProgressivePromise();

            GenericFutureListener<ChannelProgressiveFuture> list = new ChannelProgressiveFutureListener() {

                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total)
                        throws Exception {
                    listenerExe.fireProgressListener(future, progress, total);
                }

                @Override
                public void operationComplete(ChannelProgressiveFuture future) throws Exception {

                }
            };
            progressivePromise.addListener(list);
            channelFuture = channel.writeAndFlush(request, progressivePromise);
        } else {
            channelFuture = channel.writeAndFlush(request);
        }

        if (syn) {
            try {
                channelFuture.sync();
            } catch (Exception e) {
                clearResultMap(requestId, response);
                listenerExe.fireException(wrapperException(e));
                return;
            }
            boolean success = channelFuture.isSuccess();
            if (!success) {
                clearResultMap(requestId, response);
                listenerExe.fireException(wrapperException(channelFuture.cause()));
                return;
            }
            listenerExe.fireComplete();

            if (ack) {
                try {
                    response.waitForAck(getConfiguration().getResponseTimeoutMillis(), TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    clearResultMap(requestId, response);
                    listenerExe.fireException(wrapperException(e));
                    return;
                }
                listenerExe.fireAck();
            }

            if (required) {
                Object responseValue;
                try {
                    responseValue = response.waitForResponse(getConfiguration().getResponseTimeoutMillis(), TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    clearResultMap(requestId, response);
                    listenerExe.fireException(wrapperException(e));
                    return;
                }
                listenerExe.fireResponseListener(responseValue);
            }
            clearResultMap(requestId, response);
        } else {
            channelFuture.addListener(future -> {
                boolean success = future.isSuccess();
                if (!success) {
                    clearResultMap(requestId, response);
                    listenerExe.fireException(wrapperException(channelFuture.cause()));
                    return;
                }
                listenerExe.fireComplete();
                if (response != null) {
                    sendWorker.submit(() -> {
                        if (ack) {
                            try {
                                response.waitForAck(getConfiguration().getResponseTimeoutMillis(), TimeUnit.MILLISECONDS);
                            } catch (Exception e) {
                                clearResultMap(requestId, response);
                                listenerExe.fireException(wrapperException(e));
                                return;
                            }
                            listenerExe.fireAck();
                        }

                        if (required) {
                            Object responseValue;
                            try {
                                responseValue = response.waitForResponse(getConfiguration().getResponseTimeoutMillis(), TimeUnit.MILLISECONDS);
                            } catch (Exception e) {
                                clearResultMap(requestId, response);
                                listenerExe.fireException(wrapperException(e));
                                return;
                            }

                            listenerExe.fireResponseListener(responseValue);
                        }

                        clearResultMap(requestId, response);
                    });
                }
            });
        }
    }

    public Throwable wrapperException(Throwable e) {
        if (e instanceof TimeoutException || e instanceof NetworkException) {
            return e;
        }

        if (e instanceof ClosedChannelException) {
            return new NetworkException(e);
        }

        if (e instanceof io.netty.handler.timeout.TimeoutException) {
            return new TimeoutException(e);
        }
        return e;
    }

    public abstract Channel getChannel(NettyRequest request);

    public void addDelayList(RequestContext requestContext, Throwable e) {

        if (!sendListOnConnected.contains(requestContext)) {

            logger.info("add delay list : {} {}", requestContext.getRequest().getClient(), requestContext);

            if (requestContext.getRequest().getAck()) {
                requestContext.getListeners().add(new MessageListener() {
                    @Override
                    public void onAck() {
                        sendListOnConnected.remove(requestContext);
                    }
                });

            } else {
                requestContext.getListeners().add(new MessageListener() {
                    @Override
                    public void onComplete() {
                        sendListOnConnected.remove(requestContext);
                    }
                });
            }

            sendListOnConnected.addIfAbsent(requestContext);
        }
    }

    private void clearResultMap(String requestId, NettyResponse response) {
        if (requestId != null && response != null) {
            responseMap.remove(requestId);
        }
    }

    class SendListOnConnectedTask implements Runnable {

        @Override
        public void run() {
            try {
                for (RequestContext requestContext : sendListOnConnected) {

                    if (requestContext.getBindChannel() != null && !requestContext.getBindChannel().isActive()) {
                        sendListOnConnected.remove(requestContext);
                    }

                    Channel channel = requestContext.getBindChannel() != null ? requestContext.getBindChannel()
                            : getChannel(requestContext.getRequest());
                    if (channel != null) {
                        sendRequest(channel, requestContext.getRequest(), requestContext.getListeners());
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private static class MessageListenerExecutor {

        private final List<MessageListener> listeners;

        private final boolean hasResponse;

        private final boolean hasProgress;

        public MessageListenerExecutor(List<MessageListener> listeners) {
            this.listeners = listeners;
            this.hasResponse = hasMethod("onResponse", Object.class);
            this.hasProgress = hasMethod("onProgress", Object.class, long.class, long.class);
        }

        public boolean hasResponseListener() {
            return hasResponse;
        }

        public boolean hasProgressListener() {
            return hasProgress;
        }

        public void fireAck() {
            for (MessageListener listener : listeners) {
                listener.onAck();
            }
        }

        public void fireException(Throwable e) {
            for (MessageListener listener : listeners) {
                listener.onException(e);
            }
        }

        public void fireComplete() {
            for (MessageListener listener : listeners) {
                listener.onComplete();
            }
        }

        public void fireResponseListener(Object response) {
            for (MessageListener listener : listeners) {
                listener.onResponse(response);
            }
        }

        public void fireProgressListener(Object future, long progress, long total) {
            for (MessageListener listener : listeners) {
                listener.onProgress(future, progress, total);
            }
        }

        private boolean hasMethod(String methodName, Class<?>... parameterTypes) {
            for (MessageListener listener : listeners) {
                Method method = null;
                try {
                    method = listener.getClass().getDeclaredMethod(methodName, parameterTypes);
                } catch (Exception e) {
                    // ignore;
                }

                if (method != null && !method.isDefault()) {
                    return true;
                }
            }
            return false;
        }
    }

    public void setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    public RequestHandler getRequestHandler() {
        return requestHandler;
    }

    public SSLHandler getSsl() {
        return ssl;
    }

    public void setSsl(SSLHandler ssl) {
        this.ssl = ssl;
    }

    public RequestId getRequestId() {
        return requestId;
    }

    public void setRequestId(RequestId requestId) {
        this.requestId = requestId;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public MessageSerialFactory getMessageSerialFactory() {
        return messageSerialFactory;
    }

    public void setMessageSerialFactory(MessageSerialFactory messageSerialFactory) {
        this.messageSerialFactory = messageSerialFactory;
    }
}
