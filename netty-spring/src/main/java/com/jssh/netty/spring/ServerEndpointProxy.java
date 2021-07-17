package com.jssh.netty.spring;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jssh.netty.server.Server;

public class ServerEndpointProxy<T> implements InvocationHandler {

	private Map<Method, ServerEndpointMethodExecutor> cache = new ConcurrentHashMap<>();

	private Server server;

	public ServerEndpointProxy(Class<T> endPointInterface, Server server) {
		this.server = server;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			if (Object.class.equals(method.getDeclaringClass())) {
				return method.invoke(this, args);
			} else if (isDefaultMethod(method)) {
				return invokeDefaultMethod(proxy, method, args);
			}
		} catch (Throwable t) {
			throw t;
		}

		return doInvoke(proxy, method, args);
	}

	private Object doInvoke(Object proxy, Method method, Object[] args) throws Throwable {
		ServerEndpointMethodExecutor serverEndpointMethodExecutor = cache.get(method);
		if (serverEndpointMethodExecutor == null) {
			serverEndpointMethodExecutor = new ServerEndpointMethodExecutor(method, server);
			cache.put(method, serverEndpointMethodExecutor);
		}
		return serverEndpointMethodExecutor.invoke(args);
	}

	private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
		final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
				.getDeclaredConstructor(Class.class, int.class);
		if (!constructor.isAccessible()) {
			constructor.setAccessible(true);
		}
		final Class<?> declaringClass = method.getDeclaringClass();
		return constructor
				.newInstance(declaringClass,
						MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED | MethodHandles.Lookup.PACKAGE
								| MethodHandles.Lookup.PUBLIC)
				.unreflectSpecial(method, declaringClass).bindTo(proxy).invokeWithArguments(args);
	}

	private boolean isDefaultMethod(Method method) {
		return (method.getModifiers() & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC
				&& method.getDeclaringClass().isInterface();
	}
}
