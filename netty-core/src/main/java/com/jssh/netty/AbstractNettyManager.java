package com.jssh.netty;

import com.jssh.netty.exception.BizException;
import com.jssh.netty.exception.NettyException;
import com.jssh.netty.exception.NetworkException;
import com.jssh.netty.exception.TimeoutException;
import com.jssh.netty.filter.NettyRequestChains;
import com.jssh.netty.filter.NettyRequestFilter;
import com.jssh.netty.handler.DefaultNettyRequestHandler;
import com.jssh.netty.handler.RequestHandler;
import com.jssh.netty.handler.ReturnValue;
import com.jssh.netty.listener.MessageListener;
import com.jssh.netty.listener.NettyResponse;
import com.jssh.netty.request.*;
import com.jssh.netty.serial.ErrorObject;
import com.jssh.netty.serial.MessageSerial;
import com.jssh.netty.ssl.SSLHandler;
import com.jssh.netty.support.NettyMessageDecoder;
import com.jssh.netty.support.NettyMessageEncoder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.*;

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

    private int executorSize = 10;
    private int responseTimeout = 600000;
    private int validTime = 600000;
    private int responseCleanRate = 7200000;

    private int bossCount = 5;
    private int workerCount = 10;

    private long readerIdleTime = 180;
    private long writerIdleTime = 120;
    private long allIdleTime = 240;
    private int writeTimeout = 600;

    private int backlog = 128;
    private int sndbuf = 32768;
    private int rcvbuf = 32768;
    private boolean keepAlive = true;
    private int connectTimeoutMillis = 10000;

    private boolean printMessage = true;
    private boolean logging = false;

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

    private MessageSerial messageSerial;

    public void initExecutors() throws Exception {
        this.executor = Executors.newScheduledThreadPool(executorSize);
        this.executor.scheduleWithFixedDelay(new NettyResultClean(validTime), responseCleanRate, responseCleanRate,
                TimeUnit.MILLISECONDS);
        this.executor.scheduleWithFixedDelay(new SendListOnConnectedTask(), 30, 30, TimeUnit.SECONDS);
        this.receiveWorker = Executors.newCachedThreadPool();
        this.sendWorker = Executors.newCachedThreadPool();
    }

    @Override
    public void start() throws Exception {
        initExecutors();
        initRequestFilter();
        initSSL();
        initBootstrap();
        startConnection();
    }

    @Override
    public void stop() {
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
        ssl.initSSL();
    }

    protected void initBootstrap() {

    }

    protected void initSocketChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        // 处理日志
        if (logging) {
            pipeline.addLast(new LoggingHandler(LogLevel.INFO));
        }
        SSLEngine sslEngine = createSSLEngine();
        if (sslEngine != null) {
            pipeline.addLast("ssl", new SslHandler(sslEngine));
        }

        // 处理心跳
        pipeline.addLast(
                new IdleStateHandler(getReaderIdleTime(), getWriterIdleTime(), getAllIdleTime(), TimeUnit.SECONDS));

        pipeline.addLast(new LengthFieldPrepender(4, false));
        pipeline.addLast(new LengthFieldBasedFrameDecoder(2048 * 1024, 0, 4, 0, 4));
        pipeline.addLast("streamer", new ChunkedWriteHandler());
        pipeline.addLast("decoder", new NettyMessageDecoder(messageSerial));
        pipeline.addLast("encoder", new NettyMessageEncoder(messageSerial));
        pipeline.addLast("writeTO", new WriteTimeoutHandler(getWriteTimeout()));
        pipeline.addLast("handler", handler);
    }

    protected SSLEngine createSSLEngine() {
        return ssl.createSSLEngine();
    }

    protected void startConnection() throws Exception {

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

        if (printMessage) {
            logger.info("<<--- receiving message [{}] {}", request.getClientInfo(), request);
        }

        String responseId = request.getResponseId();
        if (responseId != null) {
            doResponseHandle(responseId, request);
            return;
        }

        String requestId = request.getRequestId();
        Boolean ack = request.getAck();
        if (requestId != null && ack != null && ack) {
            NettyRequest ackRequest = RequestBuilder.builder().setResponseId(requestId).setSyn(false).setAck(false)
                    .setRequestAction(ACK_REQUEST).build();
            ackRequest.setClientInfo(request.getClientInfo());
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

        checkActionRequest(ctx, request);

        ReturnValue responseValue = null;
        NettyRequest response = null;
        try {
            responseValue = requestHandler.handleRequest(ctx, request);
        } catch (Exception e) {
            while (e instanceof InvocationTargetException && e.getCause() != null) {
                e = (Exception) e.getCause();
            }

            logger.error(e.getMessage(), e);
            response = RequestBuilder.builder().setResponseId(request.getRequestId()).setSyn(false).setAck(false)
                    .setRequestAction(ERROR_REQUEST).setBody(new ErrorObject(e)).build();
        }

        if (responseValue != null && responseValue.isReturn()) {
            response = RequestBuilder.builder().setResponseId(request.getRequestId()).setSyn(false).setAck(false)
                    .setRequestAction(RESPONSE_REQUEST).setBody(responseValue.getReturnValue()).build();
        }

        Boolean requireResponse = (Boolean) request.getHeader("requireResponse");
        if (response == null && requireResponse != null && requireResponse) {
            response = RequestBuilder.builder().setResponseId(request.getRequestId()).setSyn(false).setAck(false)
                    .setRequestAction(RESPONSE_REQUEST).setBody(null).build();
        }

        if (response != null) {
            response.setClientInfo(request.getClientInfo());
            sendMessage(response, null);
        }
    }

    public void onConnection(ChannelHandlerContext ctx) {

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
                logger.info("---> PING");
                ctx.channel().writeAndFlush(
                        RequestBuilder.builder().setSyn(false).setAck(false).setRequestAction(PING_REQUEST).build());
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

    protected void checkActionRequest(ChannelHandlerContext ctx, NettyRequest request) {
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

        if (request.getSyn() == null) {
            request.setSyn(false);
        }

        if (request.getAck() == null) {
            request.setAck(false);
        }

        if (request.getSyn()) {

            if (requestContext.isReSendOnNetWorkException() || request.getAck()) {

                Throwable[] exp = new Throwable[1];
                requestContext.getListeners().add(new MessageListener() {
                    @Override
                    public void onException(Throwable e) {
                        exp[0] = e;
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
                    exp[0] = null;

                    Channel bindChannel = requestContext.getBindChannel();

                    sendRequest(bindChannel != null ? bindChannel : getChannel(request), requestContext.getRequest(),
                            requestContext.getListeners());

                } while ((requestContext.isReSendOnNetWorkException() && exp[0] instanceof NetworkException
                        || request.getAck() && exp[0] instanceof TimeoutException) && retry < 10);

                if (exp[0] != null) {
                    throw new NettyException(exp[0]);
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

        if (printMessage) {
            logger.info("---> send message [{}] {}", request.getClientInfo(), request);
        }

        Boolean syn = request.getSyn();
        Boolean ack = request.getAck();

        boolean requireResponse = listenerExe.hasResponseListener();

        if (requireResponse) {
            Object r = request.getHeader("requireResponse");
            if (r == null) {
                request.putHeader("requireResponse", true);
            }
        }

        if ((ack || requireResponse) && requestId == null) {
            throw new BizException("requestId can't be null");
        }

        final NettyResponse response = (ack || requireResponse) ? new NettyResponse(requestId, ack, requireResponse, channel.id().asLongText())
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
                listenerExe.fireException(new NetworkException(e));
                return;
            }
            boolean success = channelFuture.isSuccess();
            if (printMessage && requestId != null) {
                logger.info("request {} {} ", requestId, success);
            }
            if (!success) {
                clearResultMap(requestId, response);
                listenerExe.fireException(new NetworkException(channelFuture.cause()));
                return;
            }
            listenerExe.fireComplete();

            if (ack) {
                try {
                    response.waitForAck(60, TimeUnit.SECONDS);
                } catch (Exception e) {
                    clearResultMap(requestId, response);
                    listenerExe.fireException(e);
                    return;
                }
                listenerExe.fireAck();
            }

            if (requireResponse) {
                Object _response = null;
                try {
                    _response = response.waitForResponse(responseTimeout, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    clearResultMap(requestId, response);
                    listenerExe.fireException(e);
                    return;
                }
                listenerExe.fireResponseListener(_response);
            }
            clearResultMap(requestId, response);
        } else {
            channelFuture.addListener(future -> {
                boolean success = future.isSuccess();
                if (printMessage && requestId != null) {
                    logger.info("request {} {} ", requestId, success);
                }
                if (!success) {
                    clearResultMap(requestId, response);
                    listenerExe.fireException(new NetworkException(channelFuture.cause()));
                    return;
                }
                listenerExe.fireComplete();
                if (response != null) {
                    sendWorker.submit(() -> {
                        if (ack) {
                            try {
                                response.waitForAck(60, TimeUnit.SECONDS);
                            } catch (Exception e) {
                                clearResultMap(requestId, response);
                                listenerExe.fireException(e);
                                return;
                            }
                            listenerExe.fireAck();
                        }

                        if (requireResponse) {
                            Object _response = null;
                            try {
                                _response = response.waitForResponse(responseTimeout, TimeUnit.MILLISECONDS);
                            } catch (Exception e) {
                                clearResultMap(requestId, response);
                                listenerExe.fireException(e);
                                return;
                            }

                            listenerExe.fireResponseListener(_response);
                        }

                        clearResultMap(requestId, response);
                    });
                }
            });
        }
    }

    public abstract Channel getChannel(NettyRequest request);

    public void addDelayList(RequestContext requestContext, Throwable e) {

        if (!sendListOnConnected.contains(requestContext)) {

            logger.info("add delay list : {} {}", requestContext.getRequest().getClientInfo(), requestContext);

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

    class NettyResultClean implements Runnable {

        private final int validTime;

        public NettyResultClean(int validTime) {
            this.validTime = validTime;
        }

        @Override
        public void run() {
            long timeMillis = System.currentTimeMillis();
            for (Entry<String, NettyResponse> response : responseMap.entrySet()) {
                if (timeMillis - response.getValue().getStartTime().getTime() > validTime) {
                    logger.info("remove response map {}", response.getKey());
                    responseMap.remove(response.getKey());
                }
            }
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

    @Override
    public void close() {
        stop();
    }

    public void setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    public RequestHandler getRequestHandler() {
        return requestHandler;
    }

    public int getExecutorSize() {
        return executorSize;
    }

    public void setExecutorSize(int executorSize) {
        this.executorSize = executorSize;
    }

    public int getResponseTimeout() {
        return responseTimeout;
    }

    public void setResponseTimeout(int responseTimeout) {
        this.responseTimeout = responseTimeout;
    }

    public int getValidTime() {
        return validTime;
    }

    public void setValidTime(int validTime) {
        this.validTime = validTime;
    }

    public int getResponseCleanRate() {
        return responseCleanRate;
    }

    public void setResponseCleanRate(int responseCleanRate) {
        this.responseCleanRate = responseCleanRate;
    }

    public int getBossCount() {
        return bossCount;
    }

    public void setBossCount(int bossCount) {
        this.bossCount = bossCount;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

    public long getReaderIdleTime() {
        return readerIdleTime;
    }

    public void setReaderIdleTime(long readerIdleTime) {
        this.readerIdleTime = readerIdleTime;
    }

    public long getWriterIdleTime() {
        return writerIdleTime;
    }

    public void setWriterIdleTime(long writerIdleTime) {
        this.writerIdleTime = writerIdleTime;
    }

    public long getAllIdleTime() {
        return allIdleTime;
    }

    public void setAllIdleTime(long allIdleTime) {
        this.allIdleTime = allIdleTime;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public int getSndbuf() {
        return sndbuf;
    }

    public void setSndbuf(int sndbuf) {
        this.sndbuf = sndbuf;
    }

    public int getRcvbuf() {
        return rcvbuf;
    }

    public void setRcvbuf(int rcvbuf) {
        this.rcvbuf = rcvbuf;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public boolean isPrintMessage() {
        return printMessage;
    }

    public void setPrintMessage(boolean printMessage) {
        this.printMessage = printMessage;
    }

    public int getWriteTimeout() {
        return writeTimeout;
    }

    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public boolean isLogging() {
        return logging;
    }

    public void setLogging(boolean logging) {
        this.logging = logging;
    }

    public SSLHandler getSsl() {
        return ssl;
    }

    public void setSsl(SSLHandler ssl) {
        this.ssl = ssl;
    }

    public MessageSerial getMessageSerial() {
        return messageSerial;
    }

    public void setMessageSerial(MessageSerial messageSerial) {
        this.messageSerial = messageSerial;
    }

    public RequestId getRequestId() {
        return requestId;
    }

    public void setRequestId(RequestId requestId) {
        this.requestId = requestId;
    }
}
