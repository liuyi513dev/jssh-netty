package com.jssh.netty.exception;

public class SerialException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;


	public SerialException(String message) {
		super(message);
	}

	public SerialException(Throwable e) {
		super(e);
	}

	public SerialException(String message, Throwable e) {
		super(message, e);
	}
}
