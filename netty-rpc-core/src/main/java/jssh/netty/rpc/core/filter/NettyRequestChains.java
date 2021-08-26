package jssh.netty.rpc.core.filter;

import io.netty.channel.ChannelHandlerContext;
import jssh.netty.rpc.core.request.NettyRequest;

import java.util.List;

public class NettyRequestChains implements NettyRequestChain {

    private List<NettyRequestFilter> filters;
    private int index;

    public NettyRequestChains(List<NettyRequestFilter> filters) {
        this.filters = filters;
        this.index = 0;
    }

    @Override
    public void doFilter(ChannelHandlerContext ctx, NettyRequest request) {
        if (index < filters.size()) {
            filters.get(index++).doFilter(this, ctx, request);
        }
    }
}
