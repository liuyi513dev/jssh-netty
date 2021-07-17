package com.jssh.netty.listener;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.jssh.netty.exception.BizException;
import com.jssh.netty.exception.NetworkException;
import com.jssh.netty.exception.TimeoutException;
import com.jssh.netty.serial.ErrorObject;

public class NettyResponse {

	private boolean requireAck;

	private boolean requireResponse;

	private String requestId;

	private CountDownLatch ackLatch;

	private CountDownLatch responseLatch;

	private String channelId;

	private Object result;

	private ErrorObject error;

	private NetworkException exception;

	private boolean hasError;

	private boolean isAck;

	private boolean isResult;

	private Date startTime;

	public NettyResponse(String requestId, boolean requireAck, boolean requireResponse) {
		this.requestId = requestId;
		this.requireAck = requireAck;
		this.requireResponse = requireResponse;
		if (requireAck) {
			this.ackLatch = new CountDownLatch(1);
		}
		if (requireResponse) {
			this.responseLatch = new CountDownLatch(1);
		}
		this.startTime = new Date();
	}

	public void waitForAck(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		ackLatch.await(timeout, unit);

		if (hasError) {
			if (exception != null) {
				throw exception;
			}
			throw new BizException("errorClass:" + error.getErrorClass() + ", errorMessage:" + error.getMessage());
		}

		if (!isAck) {
			throw new TimeoutException("ack time out. " + requestId);
		}
	}

	public Object waitForResponse(long timeout, TimeUnit unit)
			throws InterruptedException, TimeoutException, BizException, NetworkException {

		responseLatch.await(timeout, unit);
		if (hasError) {
			if (exception != null) {
				throw exception;
			}
			throw new BizException("errorClass:" + error.getErrorClass() + ", errorMessage:" + error.getMessage());
		}
		if (!isResult) {
			throw new TimeoutException("netty result read time out. " + requestId);
		}
		return result;
	}

	public Object getResult() {
		return result;
	}

	public synchronized void setAck() {
		if (isAck) {
			return;
		}
		this.isAck = true;
		ackLatch.countDown();
	}

	public synchronized void setResult(Object result) {
		if (isResult) {
			return;
		}

		this.result = result;
		this.isResult = true;
		responseLatch.countDown();

		if(requireAck) {
			this.isAck = true;
			ackLatch.countDown();
		}
	}

	public synchronized void setError(ErrorObject object) {
		if (hasError) {
			return;
		}

		this.error = object;
		this.hasError = true;
		
		if(requireAck) {
			ackLatch.countDown();
		}
		
		if(requireResponse) {
			responseLatch.countDown();
		}
	}

	public synchronized void setException(NetworkException exception) {
		if (hasError) {
			return;
		}

		this.exception = exception;
		this.hasError = true;

		if(requireAck) {
			ackLatch.countDown();
		}
		
		if(requireResponse) {
			responseLatch.countDown();
		}
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public Date getStartTime() {
		return startTime;
	}

	public boolean isRequireAck() {
		return requireAck;
	}

	public boolean isRequireResponse() {
		return requireResponse;
	}
}
