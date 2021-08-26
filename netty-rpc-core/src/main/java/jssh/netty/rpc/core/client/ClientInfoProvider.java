package jssh.netty.rpc.core.client;

import io.netty.channel.ChannelHandlerContext;

public interface ClientInfoProvider {

    Object clientInfo(ChannelHandlerContext ctx);
}
