package jssh.netty.rpc.core.server;

import io.netty.channel.ChannelHandlerContext;
import jssh.netty.rpc.core.Attributes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultServerConnectionManager implements ServerConnectionManager {

    private final Map<ClientInfo<?>, ServerConnectionGroup> channelGroup = new ConcurrentHashMap<>();

    private final Map<String, ChannelHandlerContext> allChannels = new ConcurrentHashMap<>();

    @Override
    public ChannelHandlerContext getConnection(ClientInfo<?> clientInfo) {
        if (clientInfo == null) {
            return null;
        }
        ServerConnectionGroup serverConnectionGroup = channelGroup.get(clientInfo);
        if (serverConnectionGroup != null) {
            return serverConnectionGroup.get();
        }
        return null;
    }

    @Override
    public void newConnection(ClientInfo<?> clientInfo, ChannelHandlerContext ctx) {
        ctx.channel().attr(Attributes.CLIENT_INFO).set(clientInfo);
        ServerConnectionGroup serverConnectionGroup = channelGroup.computeIfAbsent(clientInfo, s -> new ServerConnectionGroup());
        serverConnectionGroup.add(ctx);

        allChannels.put(ctx.channel().id().asShortText(), ctx);
    }

    @Override
    public ClientInfo<?> onConnectionClose(ChannelHandlerContext ctx) {
        ClientInfo<?> clientInfo = ctx.channel().attr(Attributes.CLIENT_INFO).get();
        if (clientInfo != null) {
            ServerConnectionGroup serverConnectionGroup = channelGroup.get(clientInfo);
            if (serverConnectionGroup != null) {
                serverConnectionGroup.remove(ctx);
            }
        }

        allChannels.remove(ctx.channel().id().asShortText());
        return clientInfo;
    }

    @Override
    public void close(ChannelHandlerContext ctx) {
        ClientInfo<?> clientInfo = ctx.channel().attr(Attributes.CLIENT_INFO).get();
        if (clientInfo != null) {
            ServerConnectionGroup serverConnectionGroup = channelGroup.get(clientInfo);
            if (serverConnectionGroup != null) {
                serverConnectionGroup.close(ctx);
            }
        }

        allChannels.remove(ctx.channel().id().asShortText());
    }

    @Override
    public void closeAll() {
        for (ChannelHandlerContext ctx : allChannels.values()) {
            ctx.close();
        }
    }
}
