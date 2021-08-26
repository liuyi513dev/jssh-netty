package jssh.netty.rpc.core.request;

public class SimpleNettyRequest extends BaseNettyRequest implements NettyRequest {

    public SimpleNettyRequest() {
    }

    public SimpleNettyRequest(NettyRequest request) {
        super(request);
    }

    @Override
    public NettyRequest cloneRequest() {
        return new SimpleNettyRequest(this);
    }
}
