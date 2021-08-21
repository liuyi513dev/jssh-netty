package com.jssh.netty.request;

import com.jssh.netty.serial.BodyBuf;

public interface BufNettyRequest extends NettyRequest {

    BodyBuf getBodyBuf();
}
