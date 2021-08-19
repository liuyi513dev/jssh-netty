package com.jssh.netty.server;

import com.jssh.netty.AbstractNettyManager;
import com.jssh.netty.listener.MessageListener;
import com.jssh.netty.request.BaseNettyRequest;
import com.jssh.netty.request.NettyRequest;
import com.jssh.netty.request.RequestBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultServerNettyManager extends AbstractNettyManager implements Server {

    private static final Logger logger = LoggerFactory.getLogger(DefaultServerNettyManager.class);

    private ServerBootstrap serverBootstrap;

    private InetSocketAddress tcpPort;

    private ChannelFuture serverChannelFuture;

    private final ConcurrentHashMap<String, ClientChannel> channels = new ConcurrentHashMap<>();

    private final List<ServerNettyListener> listeners = new CopyOnWriteArrayList<>();

    private ClientValidator clientValidator;

    @Override
    public void initBootstrap() {
        ServerBootstrap b = new ServerBootstrap();
        b.group(new NioEventLoopGroup(getConfiguration().getBossCount()), new NioEventLoopGroup(getConfiguration().getWorkerCount()))
                .channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                initSocketChannel(ch);
            }

        });

        b.option(ChannelOption.SO_BACKLOG, getConfiguration().getBacklog()); // 设置TCP缓冲区
        b.option(ChannelOption.SO_SNDBUF, getConfiguration().getSndbuf()); // 设置发送数据缓冲大小
        b.option(ChannelOption.SO_RCVBUF, getConfiguration().getRcvbuf()); // 设置接受数据缓冲大小
        b.option(ChannelOption.SO_KEEPALIVE, getConfiguration().isKeepAlive());
        b.childOption(ChannelOption.SO_KEEPALIVE, getConfiguration().isKeepAlive());

        this.serverBootstrap = b;
    }

    @Override
    public void initRequestFilter() {
        addNettyRequestFilter((chain, ctx, request) -> {
            ClientChannel clientChannel = getClientChannel(ctx);
            if (clientChannel != null) {
                request.setClientInfo(clientChannel.getClientInfo());
            }
        });
    }

    private ClientChannel getClientChannel(ChannelHandlerContext ctx) {
        return channels.get(ctx.channel().id().asLongText());
    }

    @Override
    public void startConnection() throws Exception {
        logger.info("netty listen {}", tcpPort);
        this.serverChannelFuture = this.serverBootstrap.bind(this.tcpPort).sync();
        for (ServerNettyListener listener : listeners) {
            try {
                listener.onServerStart(this.serverChannelFuture);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void stopConnection() {
        logger.info("shutdown netty manager...");
        this.serverChannelFuture.channel().close();
        for (ServerNettyListener list : listeners) {
            try {
                list.onServerStop(this.serverChannelFuture);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        this.serverBootstrap.config().group().shutdownGracefully().syncUninterruptibly();
        this.serverBootstrap.config().childGroup().shutdownGracefully().syncUninterruptibly();
    }

    @Override
    protected void checkActionRequest(ChannelHandlerContext ctx, NettyRequest request) {
        ClientChannel clientChannel = getClientChannel(ctx);
        if (clientChannel == null || request.getClientInfo() == null) {
            logger.error("close {}", ctx);
            ctx.close();
        }
    }

    @Override
    public void active(ChannelHandlerContext ctx) {
    }

    @Override
    public void inactive(ChannelHandlerContext ctx) {
        super.inactive(ctx);
        ClientChannel clientChannel = channels.remove(ctx.channel().id().asLongText());
        if (clientChannel != null) {
            for (ServerNettyListener list : listeners) {
                try {
                    list.onChannelDisConnected(ctx, clientChannel.getClientInfo());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void doConnectionRequest(ChannelHandlerContext ctx, NettyRequest request) {
        logger.info("new connection {}", request);
        ClientInfo<?> _clientInfo;
        try {
            _clientInfo = clientValidator.validate(request.getHeader("clientInfo"));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            logger.info("close {}", ctx);
            ctx.close();
            return;
        }
        ClientInfo<?> clientInfo = _clientInfo;
        if (clientInfo != null) {
            for (ClientChannel client : channels.values()) {
                if (Objects.equals(client.getClientInfo(), clientInfo) && !Objects.equals(client.getCtx(), ctx)) {
                    channels.remove(client.getCtx().channel().id().asLongText());
                    logger.info("close {}", client);
                    client.getCtx().close();
                }
            }
            channels.put(ctx.channel().id().asLongText(), new ClientChannel(ctx, clientInfo));

            NettyRequest connectRequest = RequestBuilder.builder().setRequestAction(CONNECT_REQUEST)
                    .putHeader("clientChannelId", request.getHeader("clientChannelId")).setAck(true).build();

            connectRequest.setClientInfo(clientInfo);
            sendMessage(ctx.channel(), connectRequest, new MessageListener() {

                @Override
                public void onAck() {
                    logger.info("connected success...{}", clientInfo);
                    onConnection(ctx);
                    Exception ex = null;
                    for (ServerNettyListener list : listeners) {
                        try {
                            list.onChannelConnected(ctx, clientInfo);
                        } catch (Exception e1) {
                            logger.error(e1.getMessage(), e1);
                            ex = e1;
                        }
                    }
                    if (ex != null) {
                        ctx.close();
                    }
                }

                @Override
                public void onException(Throwable e) {
                    ctx.close();
                    logger.warn("connected fail...{}", clientInfo);
                    if (e != null) {
                        logger.error(e.getMessage(), e);
                    }
                }
            });
        } else {
            logger.info("close {}", ctx);
            ctx.close();
        }
    }

    @Override
    public void onConnection(ChannelHandlerContext ctx) {
    }

    @Override
    public Channel getChannel(NettyRequest request) {
        ClientInfo<?> clientInfo = request.getClientInfo();
        if (clientInfo == null) {
            return null;
        }
        for (ClientChannel client : channels.values()) {
            if (Objects.equals(client.getClientInfo(), clientInfo) && client.getCtx().channel().isActive()) {
                return client.getCtx().channel();
            }
        }
        return null;
    }

    @Override
    public void addListener(ServerNettyListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(ServerNettyListener listener) {
        this.listeners.remove(listener);
    }

    public void setServerBootstrap(ServerBootstrap serverBootstrap) {
        this.serverBootstrap = serverBootstrap;
    }

    public void setTcpPort(InetSocketAddress tcpPort) {
        this.tcpPort = tcpPort;
    }

    public ClientValidator getClientValidator() {
        return clientValidator;
    }

    public void setClientValidator(ClientValidator clientValidator) {
        this.clientValidator = clientValidator;
    }
}
