package jssh.netty.rpc.core.request;

import io.netty.channel.Channel;
import jssh.netty.rpc.core.listener.MessageListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RequestContext {

    private NettyRequest request;

    private boolean reSendOnNetWorkException;

    private Channel bindChannel;

    private List<MessageListener> listeners = new CopyOnWriteArrayList<MessageListener>();

    private Integer retryCount;

    public RequestContext(NettyRequest request, boolean reSendOnNetWorkException, Channel bindChannel,
                          MessageListener listener) {
        this.request = request;
        this.reSendOnNetWorkException = reSendOnNetWorkException;
        this.bindChannel = bindChannel;
        if (listener != null) {
            this.listeners.add(listener);
        }
    }

    public NettyRequest getRequest() {
        return request;
    }

    public void setRequest(NettyRequest request) {
        this.request = request;
    }

    public Channel getBindChannel() {
        return bindChannel;
    }

    public void setBindChannel(Channel bindChannel) {
        this.bindChannel = bindChannel;
    }

    public List<MessageListener> getListeners() {
        return listeners;
    }

    public void setListeners(List<MessageListener> listeners) {
        this.listeners = listeners;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public boolean isReSendOnNetWorkException() {
        return reSendOnNetWorkException;
    }

    public void setReSendOnNetWorkException(boolean reSendOnNetWorkException) {
        this.reSendOnNetWorkException = reSendOnNetWorkException;
    }
}
