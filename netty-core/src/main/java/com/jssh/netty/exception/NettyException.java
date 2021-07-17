package com.jssh.netty.exception;

public class NettyException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public NettyException(String message) {
		super(message);
	}
	
	public NettyException(Throwable e) {
		super(e);
	}
	
	public NettyException(String message, Throwable e) {
		super(message, e);
	}
}
