package jssh.netty.rpc.core.server;

import io.netty.channel.ChannelHandlerContext;

public interface ServerConnectionManager {

    ChannelHandlerContext getConnection(ClientInfo<?> clientInfo);

    void newConnection(ClientInfo<?> clientInfo, ChannelHandlerContext ctx);

    ClientInfo<?> onConnectionClose(ChannelHandlerContext ctx);

    void close(ChannelHandlerContext ctx);

    void closeAll();
}
