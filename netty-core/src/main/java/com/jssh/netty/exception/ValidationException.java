package com.jssh.netty.exception;

public class ValidationException extends RuntimeException	{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ValidationException(String code) {
		super(code);
	}
	
	public ValidationException(String code, Throwable e) {
		super(code, e);
	}
}