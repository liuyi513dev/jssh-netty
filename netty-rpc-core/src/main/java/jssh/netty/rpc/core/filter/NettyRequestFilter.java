package jssh.netty.rpc.core.filter;

import io.netty.channel.ChannelHandlerContext;
import jssh.netty.rpc.core.request.NettyRequest;

public interface NettyRequestFilter {

    void doFilter(NettyRequestChain chain, ChannelHandlerContext ctx, NettyRequest request);
}
