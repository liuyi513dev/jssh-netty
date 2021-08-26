package jssh.netty.rpc.core.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jssh.netty.rpc.core.AbstractNettyManager;
import jssh.netty.rpc.core.Attributes;
import jssh.netty.rpc.core.listener.MessageListener;
import jssh.netty.rpc.core.request.NettyRequest;
import jssh.netty.rpc.core.request.RequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultServerNettyManager extends AbstractNettyManager implements Server {

    private static final Logger logger = LoggerFactory.getLogger(DefaultServerNettyManager.class);

    private ServerBootstrap serverBootstrap;

    private ServerBindInfo serverBindInfo;

    private ChannelFuture serverBindChannel;

    private final List<ServerNettyListener> listeners = new CopyOnWriteArrayList<>();

    private ClientValidator clientValidator;

    private final ServerConnectionManager serverConnectionManager = new DefaultServerConnectionManager();

    @Override
    public void initBootstrap() {
        if (serverBootstrap != null) {
            return;
        }

        ServerBootstrap b = new ServerBootstrap();
        b.group(new NioEventLoopGroup(getConfiguration().getBossCount()), new NioEventLoopGroup(getConfiguration().getWorkerCount()))
                .channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                initSocketChannel(ch);
            }

        });

        b.option(ChannelOption.SO_BACKLOG, getConfiguration().getBacklog());
        b.option(ChannelOption.SO_SNDBUF, getConfiguration().getSndbuf());
        b.option(ChannelOption.SO_RCVBUF, getConfiguration().getRcvbuf());
        b.option(ChannelOption.SO_KEEPALIVE, getConfiguration().isKeepAlive());
        b.childOption(ChannelOption.SO_KEEPALIVE, getConfiguration().isKeepAlive());

        this.serverBootstrap = b;
    }

    @Override
    public void initRequestFilter() {
        addNettyRequestFilter((chain, ctx, request) -> {
            ClientInfo<?> clientInfo = ctx.channel().attr(Attributes.CLIENT_INFO).get();
            request.setClient(clientInfo);
        });
    }

    @Override
    public void initConnection() {
        logger.info("netty listen {}", serverBindInfo.getChannelSocket());
        this.serverBindChannel = this.serverBootstrap.bind(serverBindInfo.getChannelSocket());
        this.serverBindChannel.addListener(future -> {
            for (ServerNettyListener listener : listeners) {
                try {
                    listener.onServerStart(serverBindChannel);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public void stopConnection() {
        logger.info("shutdown netty manager...");
        serverConnectionManager.closeAll();
        for (ServerNettyListener list : listeners) {
            try {
                list.onServerStop(this.serverBindChannel);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        this.serverBindChannel.channel().close();
        this.serverBootstrap.config().group().shutdownGracefully().syncUninterruptibly();
        this.serverBootstrap.config().childGroup().shutdownGracefully().syncUninterruptibly();
    }

    @Override
    public void active(ChannelHandlerContext ctx) {
    }

    @Override
    public void inactive(ChannelHandlerContext ctx) {
        super.inactive(ctx);
        ClientInfo<?> clientInfo = serverConnectionManager.onConnectionClose(ctx);
        if (clientInfo != null) {
            for (ServerNettyListener list : listeners) {
                try {
                    list.onChannelDisConnected(ctx, clientInfo);
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
            _clientInfo = clientValidator.validate(ctx, request.getHeader("clientInfo"));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            logger.info("close {}", ctx);
            ctx.close();
            return;
        }
        ClientInfo<?> clientInfo = _clientInfo;
        if (clientInfo != null) {
            ctx.channel().attr(Attributes.CHANNEL_NAME).set(_clientInfo.toString());
            serverConnectionManager.newConnection(clientInfo, ctx);
            NettyRequest connectRequest = RequestBuilder.builder().setRequestAction(CONNECT_REQUEST)
                    .putHeader("clientChannelId", request.getHeader("clientChannelId")).setAck(true).build();
            connectRequest.setClient(clientInfo);
            sendMessage(ctx.channel(), connectRequest, new MessageListener() {

                @Override
                public void onAck() {
                    logger.info("connected success...{}", clientInfo);
                    onConnectionSuccess(ctx);
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
    public void onConnectionSuccess(ChannelHandlerContext ctx) {
    }

    @Override
    public Channel getChannel(NettyRequest request) {
        ClientInfo<?> clientInfo = request.getClient();
        return getChannel(clientInfo);
    }

    @Override
    public Channel getChannel(ClientInfo<?> clientInfo) {
        ChannelHandlerContext ctx = serverConnectionManager.getConnection(clientInfo);
        return ctx != null ? ctx.channel() : null;
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

    public ServerBindInfo getServerBindInfo() {
        return serverBindInfo;
    }

    public void setServerBindInfo(ServerBindInfo serverBindInfo) {
        this.serverBindInfo = serverBindInfo;
    }

    public ClientValidator getClientValidator() {
        return clientValidator;
    }

    public void setClientValidator(ClientValidator clientValidator) {
        this.clientValidator = clientValidator;
    }
}
