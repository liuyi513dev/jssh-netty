package com.jssh.netty.exception;

public class NotSupportMessageTypes extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NotSupportMessageTypes() {
	}

	public NotSupportMessageTypes(String message) {
		super(message);
	}

	public NotSupportMessageTypes(String message, Throwable e) {
		super(message, e);
	}
}
