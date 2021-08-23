package com.jssh.netty.serial;

public class ErrorObject {

	private String message;
	
	private String errorClass;
	
	public ErrorObject() {
		
	}
	
	public ErrorObject(Throwable e) {
		this.message = e.getMessage();
		this.errorClass = e.getClass().toString();
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getErrorClass() {
		return errorClass;
	}

	public void setErrorClass(String errorClass) {
		this.errorClass = errorClass;
	}
}
