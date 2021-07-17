package com.jssh.netty.exception;

public class ActionNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ActionNotFoundException() {
	}

	public ActionNotFoundException(String message) {
		super(message);
	}

	public ActionNotFoundException(String message, Throwable e) {
		super(message, e);
	}
}
