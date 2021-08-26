package jssh.netty.rpc.spring;

import jssh.netty.rpc.core.client.Client;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientEndpointProxy<T> implements InvocationHandler {

    private Client client;

    private Map<Method, ClientEndpointMethodExecutor> cache = new ConcurrentHashMap<>();

    public ClientEndpointProxy(Class<T> endPointInterface, Client client) {
        this.client = client;
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
        ClientEndpointMethodExecutor clientEndpointMethodExecutor = cache.get(method);
        if (clientEndpointMethodExecutor == null) {
            clientEndpointMethodExecutor = new ClientEndpointMethodExecutor(method, client);
            cache.put(method, clientEndpointMethodExecutor);
        }
        return clientEndpointMethodExecutor.invoke(args);
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
