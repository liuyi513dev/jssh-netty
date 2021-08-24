package com.jssh.netty.request;

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
		return bodyBuf != null ? new BodyBufNettyRequest(super.build(), bodyBuf) : super.build();
	}
}
