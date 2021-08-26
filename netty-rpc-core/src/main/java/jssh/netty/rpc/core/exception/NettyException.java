package jssh.netty.rpc.core.exception;

public class NettyException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public NettyException() {
    }

    public NettyException(String message) {
        super(message);
    }

    public NettyException(Throwable e) {
        super(e);
    }

    public NettyException(String message, Throwable e) {
        super(message, e);
    }
}
