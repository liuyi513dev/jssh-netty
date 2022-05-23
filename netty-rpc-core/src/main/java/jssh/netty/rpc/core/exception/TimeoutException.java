package jssh.netty.rpc.core.exception;

public class TimeoutException extends NettyException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public TimeoutException(String message) {
        super(message);
    }

    public TimeoutException(String message, Throwable e) {
        super(message, e);
    }

    public TimeoutException(Throwable e) {
        super(e);
    }
}
