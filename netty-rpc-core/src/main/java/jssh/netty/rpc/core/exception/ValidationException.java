package jssh.netty.rpc.core.exception;

public class ValidationException extends NettyException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable e) {
        super(message, e);
    }
}