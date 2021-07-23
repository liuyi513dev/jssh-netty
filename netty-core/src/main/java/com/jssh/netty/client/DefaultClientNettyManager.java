package com.jssh.netty.client;

import com.jssh.netty.AbstractNettyManager;
import com.jssh.netty.request.NettyRequest;
import com.jssh.netty.request.RequestBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DefaultClientNettyManager extends AbstractNettyManager implements Runnable, Client {

    private static final Logger logger = LoggerFactory.getLogger(DefaultClientNettyManager.class);

    private Bootstrap bootstrap;

    private InetSocketAddress tcpPort;

    private volatile ChannelFuture channelFuture;

    private volatile boolean valid;
    private volatile CountDownLatch validLatch;

    private List<ClientNettyListener> listener = new CopyOnWriteArrayList<>();

    private ClientInfoProvider clientInfoProvider;

    @Override
    public void initBootstrap() {

        Bootstrap b = new Bootstrap();
        b.group(new NioEventLoopGroup(getWorkerCount())).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        initSocketChannel(ch);
                    }
                });

        // b.option(ChannelOption.SO_BACKLOG, backlog); // 设置TCP缓冲区
        b.option(ChannelOption.SO_SNDBUF, getSndbuf()); // 设置发送数据缓冲大小
        b.option(ChannelOption.SO_RCVBUF, getRcvbuf()); // 设置接受数据缓冲大小
        b.option(ChannelOption.SO_KEEPALIVE, isKeepAlive());
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getConnectTimeoutMillis());
        this.bootstrap = b;
    }

    @Override
    public void run() {
        connect();
    }

    @Override
    public void startConnection() throws Exception {
        this.executor.schedule(this, 0, TimeUnit.SECONDS);
    }

    @Override
    public void stopConnection() {
        if (channelFuture != null) {
            try {
                logger.info("close {}", channelFuture);
                channelFuture.channel().close();
            } catch (Exception e) {
            }
            channelFuture = null;
        }
        this.bootstrap.config().group().shutdownGracefully().syncUninterruptibly();
    }

    private void connect() {
        try {
            logger.info("try to connect {}:{} ", tcpPort.getAddress().getHostAddress(), tcpPort.getPort());
            this.channelFuture = bootstrap.connect(tcpPort).sync();
            checkChannelFuture(channelFuture);
            this.validLatch = new CountDownLatch(1);
            this.channelFuture.channel().closeFuture().sync();
            this.channelFuture = null;
            this.valid = false;
            this.validLatch = null;
        } catch (Exception e) {
            this.channelFuture = null;
        } finally {
            if (!this.executor.isTerminated()) {
                this.executor.schedule(this, 10, TimeUnit.SECONDS);
            }
        }
    }

    private void checkChannelFuture(ChannelFuture channelFuture) throws Exception {
        if (channelFuture.channel().localAddress().equals(channelFuture.channel().remoteAddress())) {
            logger.info("close {}", channelFuture);
            channelFuture.channel().close();
        }
    }

    @Override
    public void active(ChannelHandlerContext ctx) {
        Object clientInfo = clientInfoProvider.clientInfo();
        sendMessage(ctx.channel(), RequestBuilder.builder().setRequestAction(CONNECT_REQUEST).putHeader("clientInfo", clientInfo)
                .putHeader("clientChannelId", ctx.channel().id().asLongText()).build(), null);
    }

    @Override
    public void inactive(ChannelHandlerContext ctx) {
        logger.info("disconnected...");
        super.inactive(ctx);
        for (ClientNettyListener list : listener) {
            try {
                list.onChannelDisconnected(ctx);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void doConnectionRequest(ChannelHandlerContext ctx, NettyRequest request) {
        if (!Objects.equals(request.getHeader("clientChannelId"), ctx.channel().id().asLongText())) {
            return;
        }
        this.valid = true;
        this.validLatch.countDown();

        logger.info("netty connected.....{}", request);
        onConnection(ctx);
        for (ClientNettyListener list : listener) {
            try {
                list.onChannelConnected(ctx);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void onConnection(ChannelHandlerContext ctx) {
    }

    @Override
    public Channel getChannel(NettyRequest request) {
        if (channelFuture != null) {

            if (valid) {
                return channelFuture.channel();
            }

            if (validLatch != null) {
                try {
                    validLatch.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {

                }

                if (valid) {
                    return channelFuture.channel();
                } else {
                    channelFuture.channel().close();
                }
            }

        }
        return null;
    }

    @Override
    protected void checkActionRequest(ChannelHandlerContext ctx, NettyRequest request) {

    }

    @Override
    public void addListener(ClientNettyListener listener) {
        this.listener.add(listener);
    }

    @Override
    public void removeListener(ClientNettyListener listener) {
        this.listener.remove(listener);
    }

    @Override
    public NettyStatus nettyStatus() {
        Channel ch;
        if (channelFuture != null && (ch = channelFuture.channel()) != null && ch.isActive()) {
            return NettyStatus.ACTIVE;
        }
        return NettyStatus.DISCONNECTED;
    }

    public void setTcpPort(InetSocketAddress tcpPort) {
        this.tcpPort = tcpPort;
    }

    public ClientInfoProvider getClientInfoProvider() {
        return clientInfoProvider;
    }

    public void setClientInfoProvider(ClientInfoProvider clientInfoProvider) {
        this.clientInfoProvider = clientInfoProvider;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }
}
