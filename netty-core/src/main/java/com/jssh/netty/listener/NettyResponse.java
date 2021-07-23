package com.jssh.netty.listener;

import com.jssh.netty.exception.BizException;
import com.jssh.netty.exception.NetworkException;
import com.jssh.netty.exception.TimeoutException;
import com.jssh.netty.serial.ErrorObject;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NettyResponse {

    private final String requestId;

    private final CountDownLatch ackLatch;

    private final CountDownLatch responseLatch;

    private final String channelId;

    private final Date startTime;

    private Object result;

    private ErrorObject error;

    private NetworkException exception;

    private boolean hasError;

    private boolean isAck;

    private boolean isResult;

    public NettyResponse(String requestId, boolean requireAck, boolean requireResponse, String channelId) {
        this.requestId = requestId;
        this.ackLatch = requireAck ? new CountDownLatch(1) : null;
        this.responseLatch = requireResponse ? new CountDownLatch(1) : null;
        this.startTime = new Date();
        this.channelId = channelId;
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

        if (ackLatch != null) {
            this.isAck = true;
            ackLatch.countDown();
        }

        responseLatch.countDown();
    }

    public synchronized void setError(ErrorObject object) {
        if (hasError) {
            return;
        }

        this.error = object;
        this.hasError = true;

        if (ackLatch != null) {
            ackLatch.countDown();
        }

        if (responseLatch != null) {
            responseLatch.countDown();
        }
    }

    public synchronized void setException(NetworkException exception) {
        if (hasError) {
            return;
        }

        this.exception = exception;
        this.hasError = true;

        if (ackLatch != null) {
            ackLatch.countDown();
        }

        if (responseLatch != null) {
            responseLatch.countDown();
        }
    }

    public String getChannelId() {
        return channelId;
    }

    public String getRequestId() {
        return requestId;
    }

    public Date getStartTime() {
        return startTime;
    }
}
