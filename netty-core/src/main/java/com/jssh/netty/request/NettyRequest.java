package com.jssh.netty.request;

import com.jssh.netty.serial.BodyBuf;
import com.jssh.netty.server.ClientInfo;

public interface NettyRequest {

    boolean getSyn();

    boolean getAck();

    boolean getRequired();

    String getRequestId();

    String getRequestAction();

    HeaderList getHeaders();

    void putHeader(String key, Object value);

    Object getHeader(String key);

    Object getBody();

    String getResponseId();

    void setResponseId(String responseId);

    void setSyn(boolean syn);

    void setAck(boolean ack);

    void setRequired(boolean required);

    void setRequestId(String requestId);

    void setRequestAction(String requestAction);

    void setHeaders(HeaderList headers);

    void setBody(Object body);

    NettyRequest cloneRequest();

    ClientInfo<?> getClientInfo();

    void setClientInfo(ClientInfo<?> clientInfo);

    BodyBuf getBodyBuf();
}
