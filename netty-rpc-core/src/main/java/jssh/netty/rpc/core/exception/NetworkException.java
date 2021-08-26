package jssh.netty.rpc.core.exception;

public class NetworkException extends NettyException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;


    public NetworkException(String message) {
        super(message);
    }

    public NetworkException(Throwable e) {
        super(e);
    }

    public NetworkException(String message, Throwable e) {
        super(message, e);
    }
}
