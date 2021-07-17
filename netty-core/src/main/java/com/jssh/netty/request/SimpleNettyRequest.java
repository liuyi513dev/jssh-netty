package com.jssh.netty.request;

import com.jssh.netty.serial.BodyBuf;

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

	@Override
	public BodyBuf getBodyBuf() {
		return null;
	}
}
