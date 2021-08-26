package jssh.netty.rpc.core.request;

import jssh.netty.rpc.core.server.ClientInfo;

import java.beans.Transient;
import java.util.Objects;

public abstract class BaseNettyRequest implements NettyRequest {

    private boolean syn;

    private boolean ack;

    private boolean required;

    private String requestAction;

    private String requestId;

    private String responseId;

    private HeaderList headers;

    private Object body;

    private ClientInfo<?> client;

    private String server;

    public BaseNettyRequest() {
    }

    public BaseNettyRequest(NettyRequest request) {
        this.syn = request.getSyn();
        this.ack = request.getAck();
        this.required = request.getRequired();
        this.requestAction = request.getRequestAction();
        this.requestId = request.getRequestId();
        this.responseId = request.getResponseId();
        this.headers = request.getHeaders() != null ? new HeaderList(request.getHeaders()) : null;
        this.body = request.getBody();
    }

    @Override
    public boolean getSyn() {
        return syn;
    }

    @Override
    public void setSyn(boolean syn) {
        this.syn = syn;
    }

    @Override
    public boolean getAck() {
        return ack;
    }

    @Override
    public void setAck(boolean ack) {
        this.ack = ack;
    }

    @Override
    public boolean getRequired() {
        return required;
    }

    @Override
    public void setRequired(boolean required) {
        this.required = required;
    }

    @Override
    public String getRequestId() {
        return requestId;
    }

    @Override
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String getResponseId() {
        return responseId;
    }

    @Override
    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    @Override
    public String getRequestAction() {
        return requestAction;
    }

    @Override
    public void setRequestAction(String requestAction) {
        this.requestAction = requestAction;
    }

    @Override
    public HeaderList getHeaders() {
        return headers;
    }

    @Override
    public void setHeaders(HeaderList headers) {
        this.headers = headers;
    }

    @Override
    public Object getBody() {
        return body;
    }

    @Override
    public void setBody(Object body) {
        this.body = body;
    }

    @Override
    @Transient
    public ClientInfo<?> getClient() {
        return client;
    }

    @Override
    public void setClient(ClientInfo<?> client) {
        this.client = client;
    }

    @Override
    public void putHeader(String key, Object value) {
        if (headers == null) {
            headers = new HeaderList();
        }
        headers.getHeaders().add(new Header(key, value));
    }

    @Override
    public Object getHeader(String key) {
        if (headers != null && headers.getHeaders() != null) {
            for (Header h : headers.getHeaders()) {
                if (Objects.equals(h.getKey(), key)) {
                    return h.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "[" + (syn ? "syn, " : "") + (ack ? "ack, " : "") + (required ? "required, " : "")
                + (requestAction != null ? "requestAction=" + requestAction + ", " : "")
                + (requestId != null ? "requestId=" + requestId + ", " : "")
                + (responseId != null ? "responseId=" + responseId + ", " : "")
                + (headers != null ? "headers=" + headers + ", " : "")
                + (body != null ? "body=" + body + ", " : "") + (client != null ? "clientInfo=" + client : "")
                + "]";
    }
}
