package jssh.netty.rpc.core.exception;

public class TimeoutException extends NettyException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public TimeoutException(String code) {
        super(code);
    }

    public TimeoutException(String code, Throwable e) {
        super(code, e);
    }
}
