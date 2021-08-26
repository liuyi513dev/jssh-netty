package jssh.netty.rpc.core.client;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultClientConnectionManager implements ClientConnectionManager {

    private final Map<String, ChannelHandlerContext> allConnections = new ConcurrentHashMap<>();

    @Override
    public ChannelHandlerContext getConnection() {
        Optional<String> first = allConnections.keySet().stream().findFirst();
        return first.map(allConnections::get).orElse(null);
    }

    @Override
    public void newConnection(ChannelHandlerContext ctx) {
        closeAll();
        allConnections.put(ctx.channel().id().asShortText(), ctx);
    }

    @Override
    public void onConnectionClose(ChannelHandlerContext ctx) {
        allConnections.remove(ctx.channel().id().asShortText());
    }

    @Override
    public void close(ChannelHandlerContext ctx) {
        ctx.close();
        allConnections.remove(ctx.channel().id().asShortText());
    }

    @Override
    public void closeAll() {
        for (String ctxKey : allConnections.keySet()) {
            ChannelHandlerContext ctx = allConnections.remove(ctxKey);
            if (ctx != null) {
                ctx.close();
            }
        }
    }
}
