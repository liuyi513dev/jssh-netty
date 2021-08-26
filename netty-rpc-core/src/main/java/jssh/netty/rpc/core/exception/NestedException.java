package jssh.netty.rpc.core.exception;

public class NestedException extends NettyException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;


    public NestedException(String message) {
        super(message);
    }

    public NestedException(Throwable e) {
        super(e);
    }

    public NestedException(String message, Throwable e) {
        super(message, e);
    }
}
