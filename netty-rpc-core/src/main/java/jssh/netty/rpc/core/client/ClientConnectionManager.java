package jssh.netty.rpc.core.client;

import io.netty.channel.ChannelHandlerContext;

public interface ClientConnectionManager {

    ChannelHandlerContext getConnection();

    void newConnection(ChannelHandlerContext ctx);

    void onConnectionClose(ChannelHandlerContext ctx);

    void closeAll();

    void close(ChannelHandlerContext ctx);
}
