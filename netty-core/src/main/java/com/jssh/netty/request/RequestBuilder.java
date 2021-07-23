package com.jssh.netty.request;

public class RequestBuilder implements Builder {

    private boolean syn;

    private boolean ack;

    private boolean required;

    private String requestAction;

    private String requestId;

    private String responseId;

    private HeaderList headers;

    private Object body;

    public static RequestBuilder builder() {
        return new RequestBuilder();
    }

    public RequestBuilder setSyn(boolean syn) {
        this.syn = syn;
        return this;
    }

    public RequestBuilder setAck(boolean ack) {
        this.ack = ack;
        return this;
    }

    public RequestBuilder setRequired(boolean required) {
        this.required = required;
        return this;
    }

    public RequestBuilder setRequestAction(String requestAction) {
        this.requestAction = requestAction;
        return this;
    }

    public RequestBuilder setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public RequestBuilder setResponseId(String responseId) {
        this.responseId = responseId;
        return this;
    }

    public RequestBuilder setHeaders(HeaderList headers) {
        this.headers = headers;
        return this;
    }

    public RequestBuilder setBody(Object body) {
        this.body = body;
        return this;
    }

    public RequestBuilder putHeader(String key, Object value) {
        if (this.headers == null) {
            this.headers = new HeaderList();
        }
        this.headers.getHeaders().add(new Header(key, value));
        return this;
    }

    public NettyRequest build() {
        SimpleNettyRequest request = new SimpleNettyRequest();
        request.setSyn(this.syn);
        request.setAck(this.ack);
        request.setRequired(this.required);
        request.setRequestAction(this.requestAction);
        request.setRequestId(this.requestId);
        request.setResponseId(this.responseId);
        request.setHeaders(this.headers);
        request.setBody(this.body);
        return request;
    }
}
