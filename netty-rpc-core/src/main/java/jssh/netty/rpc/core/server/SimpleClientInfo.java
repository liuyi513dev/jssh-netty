package jssh.netty.rpc.core.server;

public class SimpleClientInfo<T> implements ClientInfo<T> {

    private T clientId;

    public SimpleClientInfo() {
    }

    public SimpleClientInfo(T clientId) {
        super();
        this.clientId = clientId;
    }

    @Override
    public T getClientId() {
        return clientId;
    }

    public void setClientId(T clientId) {
        this.clientId = clientId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof SimpleClientInfo))
            return false;
        SimpleClientInfo<?> other = (SimpleClientInfo<?>) obj;
        if (clientId == null) {
            if (other.clientId != null)
                return false;
        } else if (!clientId.equals(other.clientId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.valueOf(clientId);
    }
}
