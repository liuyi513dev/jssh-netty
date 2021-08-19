package com.jssh.netty.client;

import java.util.List;

public interface Client extends ClientMessageExchange {

    default void addListener(List<ClientNettyListener> listeners) {
        if (listeners != null) {
            for (ClientNettyListener listener : listeners) {
                addListener(listener);
            }
        }
    }

    void addListener(ClientNettyListener listener);

    void removeListener(ClientNettyListener listener);
}
