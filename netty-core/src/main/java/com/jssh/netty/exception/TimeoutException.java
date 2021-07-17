package com.jssh.netty.exception;

public class TimeoutException extends RuntimeException	{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public TimeoutException(String code) {
		super(code);
	}
	
	public TimeoutException(String code, Throwable e) {
		super(code, e);
	}
}
