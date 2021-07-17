package com.jssh.netty.spring;

import java.lang.reflect.Proxy;

import org.springframework.beans.factory.FactoryBean;

import com.jssh.netty.server.Server;

public class ServerEndpointFactoryBean<T> implements FactoryBean<T> {

	private Class<T> endPointInterface;

	private Server server;

	public ServerEndpointFactoryBean(Class<T> endPointInterface) {
		this.endPointInterface = endPointInterface;
	}

	@SuppressWarnings({ "rawtypes", "unchecked"})
	@Override
	public T getObject() throws Exception {
		return (T) Proxy.newProxyInstance(endPointInterface.getClassLoader(), new Class[] { endPointInterface },
				new ServerEndpointProxy(endPointInterface, server));
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

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}
}
