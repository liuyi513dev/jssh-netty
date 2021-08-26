package jssh.netty.rpc.spring;

import jssh.netty.rpc.core.client.Client;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Proxy;

public class ClientEndpointFactoryBean<T> implements FactoryBean<T> {

    private Class<T> endPointInterface;

    private Client client;

    public ClientEndpointFactoryBean(Class<T> endPointInterface) {
        this.endPointInterface = endPointInterface;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public T getObject() throws Exception {
        return (T) Proxy.newProxyInstance(endPointInterface.getClassLoader(), new Class[]{endPointInterface},
                new ClientEndpointProxy(endPointInterface, client));
    }

    @Override
    public Class<?> getObjectType() {
        return endPointInterface;
    }

    public Class<T> getEndPointInterface() {
        return endPointInterface;
    }

    public void setEndPointInterface(Class<T> endPointInterface) {
        this.endPointInterface = endPointInterface;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
