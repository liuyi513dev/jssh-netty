package jssh.netty.rpc.core.lock;

public interface Lock {

    public interface LockHandler<T> {
        void handle(T t) throws Exception;
    }

    void lock(String key, LockHandler<Object> handler) throws Exception;
}
