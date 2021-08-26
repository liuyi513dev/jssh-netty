package jssh.netty.rpc.core.server;

import io.netty.channel.Channel;

import java.util.List;

public interface Server extends ServerMessageExchange {

    default void addListeners(List<ServerNettyListener> listeners) {
        if (listeners != null) {
            for (ServerNettyListener listener : listeners) {
                addListener(listener);
            }
        }
    }

    void addListener(ServerNettyListener listener);

    void removeListener(ServerNettyListener listener);

    Channel getChannel(ClientInfo<?> clientInfo);
}
