package com.jssh.netty.request;

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
