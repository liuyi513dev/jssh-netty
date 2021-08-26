package jssh.netty.rpc.core.handler;

import jssh.netty.rpc.core.request.NettyRequest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ActionInvocation {

    private final Method method;

    private final Object target;

    private final Class<?>[] parameterTypes;

    private final Class<?> returnType;

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

    public Object getTarget() {
        return target;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public Class<?> getReturnType() {
        return returnType;
    }
}
