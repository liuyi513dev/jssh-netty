package com.jssh.netty.exception;

public class NotSupportParameterTypes extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NotSupportParameterTypes() {
	}

	public NotSupportParameterTypes(String message) {
		super(message);
	}

	public NotSupportParameterTypes(String message, Throwable e) {
		super(message, e);
	}
}
