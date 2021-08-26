package jssh.netty.rpc.core.listener;

import java.util.EventListener;

public interface MessageListener extends EventListener {

    default void onException(Throwable e) {
    }

    default void onComplete() {

    }

    default void onAck() {

    }

    default void onResponse(Object response) {

    }

    default void onProgress(Object future, long progress, long total) {

    }
}
