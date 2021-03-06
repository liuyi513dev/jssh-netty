package jssh.netty.rpc.core.client;

import io.netty.channel.Channel;

import java.util.List;

public interface Client extends ClientMessageExchange {

    default void addListeners(List<ClientNettyListener> listeners) {
        if (listeners != null) {
            for (ClientNettyListener listener : listeners) {
                addListener(listener);
            }
        }
    }

    void addListener(ClientNettyListener listener);

    void removeListener(ClientNettyListener listener);

    Channel getChannel();
}
