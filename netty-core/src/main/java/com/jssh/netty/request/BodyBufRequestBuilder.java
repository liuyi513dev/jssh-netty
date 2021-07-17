package com.jssh.netty.request;

import com.jssh.netty.serial.BodyBuf;

public class BodyBufRequestBuilder extends RequestBuilder {

	private BodyBuf bodyBuf;

	public static BodyBufRequestBuilder builder() {
		return new BodyBufRequestBuilder();
	}

	public BodyBufRequestBuilder setBodyBuf(BodyBuf bodyBuf) {
		this.bodyBuf = bodyBuf;
		return this;
	}

	@Override
	public NettyRequest build() {
		return new OriginalNettyRequest(super.build(), bodyBuf);
	};
}
