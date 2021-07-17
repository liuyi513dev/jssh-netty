package com.jssh.netty.exception;

public class BizException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public BizException(String message) {
		super(message);
	}
	
	public BizException(Throwable e) {
		super(e);
	}
	
	public BizException(String message, Throwable e) {
		super(message, e);
	}
}
