package com.jssh.netty.server;

import com.jssh.netty.exception.ValidationException;

public interface ClientValidator {

	ClientInfo<?> validate(Object param) throws ValidationException;
}
