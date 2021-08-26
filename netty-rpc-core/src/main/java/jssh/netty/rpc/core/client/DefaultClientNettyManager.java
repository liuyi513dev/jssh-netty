package jssh.netty.rpc.core.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import jssh.netty.rpc.core.AbstractNettyManager;
import jssh.netty.rpc.core.Attributes;
import jssh.netty.rpc.core.request.NettyRequest;
import jssh.netty.rpc.core.request.RequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class DefaultClientNettyManager extends AbstractNettyManager implements Client {

    private static final Logger logger = LoggerFactory.getLogger(DefaultClientNettyManager.class);

    private Bootstrap bootstrap;

    private ServerSocketInfo serverSocketInfo;

    private final List<ClientNettyListener> listener = new CopyOnWriteArrayList<>();

    private ClientInfoProvider clientInfoProvider;

    private final ClientConnectionManager clientConnectionManager = new DefaultClientConnectionManager();

    @Override
    public void initBootstrap() {
        Bootstrap b = new Bootstrap();
        b.group(new NioEventLoopGroup(getConfiguration().getWorkerCount())).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        initSocketChannel(ch);
                    }
                });

        // b.option(ChannelOption.SO_BACKLOG, backlog);
        b.option(ChannelOption.SO_SNDBUF, getConfiguration().getSndbuf());
        b.option(ChannelOption.SO_RCVBUF, getConfiguration().getRcvbuf());
        b.option(ChannelOption.SO_KEEPALIVE, getConfiguration().isKeepAlive());
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getConfiguration().getConnectTimeoutMillis());
        this.bootstrap = b;
    }

    private void addFuture(Runnable runnable, int delayMillis) {
        this.executor.schedule(runnable, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void initConnection() {
        addFuture(() -> ensureConnect(serverSocketInfo), 0);
    }

    private void ensureConnect(ServerSocketInfo channelInfo) {
        logger.info("try to connect {}:{} ",
                channelInfo.getChannelSocket().getAddress().getHostAddress(), channelInfo.getChannelSocket().getPort());
        ChannelFuture connect = bootstrap.connect(channelInfo.getChannelSocket());
        connect.channel().attr(Attributes.CHANNEL_NAME).set(channelInfo.getChannelSocket().getAddress().getHostAddress());
        connect.channel().closeFuture().addListener(future -> addFuture(() -> ensureConnect(channelInfo), 10000));
    }

    @Override
    public void stopConnection() {
        clientConnectionManager.closeAll();
        this.bootstrap.config().group().shutdownGracefully().syncUninterruptibly();
    }

    @Override
    public void active(ChannelHandlerContext ctx) {
        Object clientInfo = clientInfoProvider.clientInfo(ctx);
        sendMessage(ctx.channel(), RequestBuilder.builder().setRequestAction(CONNECT_REQUEST).putHeader("clientInfo", clientInfo)
                .putHeader("clientChannelId", ctx.channel().id().asLongText()).build(), null);
    }

    @Override
    public void inactive(ChannelHandlerContext ctx) {
        logger.info("disconnected...");
        super.inactive(ctx);
        clientConnectionManager.onConnectionClose(ctx);
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
        clientConnectionManager.newConnection(ctx);
        logger.info("netty connected.....{}", request);
        onConnectionSuccess(ctx);
        for (ClientNettyListener list : listener) {
            try {
                list.onChannelConnected(ctx);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void onConnectionSuccess(ChannelHandlerContext ctx) {
    }

    @Override
    public Channel getChannel(NettyRequest request) {
        return getChannel();
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
    public Channel getChannel() {
        ChannelHandlerContext connection = clientConnectionManager.getConnection();
        return connection != null ? connection.channel() : null;
    }

    public ServerSocketInfo getServerSocketInfo() {
        return serverSocketInfo;
    }

    public void setServerSocketInfo(ServerSocketInfo serverSocketInfo) {
        this.serverSocketInfo = serverSocketInfo;
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
