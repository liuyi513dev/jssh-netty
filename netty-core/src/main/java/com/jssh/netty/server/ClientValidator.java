package com.jssh.netty.server;

import com.jssh.netty.exception.ValidationException;
import io.netty.channel.ChannelHandlerContext;

public interface ClientValidator {

	ClientInfo<?> validate(ChannelHandlerContext ctx, Object param) throws ValidationException;
}
