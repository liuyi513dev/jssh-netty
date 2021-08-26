package jssh.netty.rpc.core.server;

import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerConnectionGroup {

    private final List<ChannelHandlerContext> channels = new CopyOnWriteArrayList<>();

    public void add(ChannelHandlerContext ctx) {
        closeAll();
        channels.add(ctx);
    }

    public ChannelHandlerContext get() {
        if (!channels.isEmpty()) {
            return channels.get(0);
        }
        return null;
    }

    public void remove(ChannelHandlerContext ctx) {
        channels.remove(ctx);
    }

    public void close(ChannelHandlerContext ctx) {
        ctx.close();
        channels.remove(ctx);
    }

    public void closeAll() {
        for (ChannelHandlerContext ctx : channels) {
            ctx.close();
        }
        channels.clear();
    }
}
