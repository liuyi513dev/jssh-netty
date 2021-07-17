package com.jssh.netty.handler;

import com.jssh.netty.request.NettyRequest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ActionInvocation {

    private Method method;

    private Object target;

    private Class<?>[] parameterTypes;

    private Class<?> returnType;

    public ActionInvocation() {
    }

    public ActionInvocation(Method method, Object target) {
        super();
        this.method = method;
        this.target = target;
        this.parameterTypes = method.getParameterTypes();
        this.returnType = method.getReturnType();
    }

    public ReturnValue invoke(NettyRequest request) throws Exception {
        Object value = doInvoke(request);
        if (value instanceof ReturnValue) {
            return (ReturnValue) value;
        }
        return new ReturnValue(returnType != void.class, value);
    }

    public Object doInvoke(NettyRequest request) throws Exception {
        return doMethodInvoke(request);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object doMethodInvoke(NettyRequest request) throws InvocationTargetException, IllegalAccessException {
        Map<String, Object> bodyArgs = (Map) request.getBody();

        List<Object> args = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        for (Parameter p : parameters) {
            if (NettyRequest.class.isAssignableFrom(p.getType())) {
                args.add(request);
            } else {
                args.add(bodyArgs.get(p.getName()));
            }
        }

        return method.invoke(target, args.size() > 0 ? args.toArray() : null);
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }
}
