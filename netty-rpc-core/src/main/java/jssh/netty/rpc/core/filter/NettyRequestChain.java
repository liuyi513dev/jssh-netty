package jssh.netty.rpc.core.filter;

import io.netty.channel.ChannelHandlerContext;
import jssh.netty.rpc.core.request.NettyRequest;

public interface NettyRequestChain {

    void doFilter(ChannelHandlerContext ctx, NettyRequest request);
}
