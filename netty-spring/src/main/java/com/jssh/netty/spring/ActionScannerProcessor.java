package com.jssh.netty.spring;

import com.jssh.netty.handler.ActionInvocation;
import com.jssh.netty.request.Action;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class ActionScannerProcessor {

    public Map<String, ActionInvocation> scan(Object bean) {

        Map<String, ActionInvocation> actionMapping = null;

        Class<?> actionClass = bean.getClass();
        while (actionClass != null) {
            Map<String, ActionInvocation> mapping = scan(actionClass, bean);
            if (mapping != null) {
                if (actionMapping == null) {
                    actionMapping = new HashMap<>(5);
                }
                actionMapping.putAll(mapping);
            }

            Class<?>[] interfaces = actionClass.getInterfaces();
            if (interfaces.length > 0) {
                for (Class<?> interfaceClass : interfaces) {
                    Map<String, ActionInvocation> interfaceMapping = scan(interfaceClass, bean);
                    if (interfaceMapping != null) {
                        if (actionMapping == null) {
                            actionMapping = new HashMap<>(5);
                        }
                        actionMapping.putAll(interfaceMapping);
                    }
                }
            }

            actionClass = actionClass.getSuperclass();
        }

        return actionMapping;
    }

    private Map<String, ActionInvocation> scan(Class<?> clazz, Object target) {
        Map<String, ActionInvocation> actionMapping = null;
        Action classAction = clazz.getAnnotation(Action.class);
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (Modifier.isPublic(method.getModifiers()) && !method.isSynthetic()) {
                Action methodAction = method.getAnnotation(Action.class);
                if (classAction != null || methodAction != null) {
                    String actionName = (classAction != null && !classAction.value().isEmpty() ?
                            classAction.value() + "." : "")
                            + (methodAction != null && !methodAction.value().isEmpty() ?
                            methodAction.value() : method.getName());

                    if (actionMapping == null) {
                        actionMapping = new HashMap<>(5);
                    }

                    actionMapping.put(actionName, new ActionInvocation(method, target));
                }
            }
        }
        return actionMapping;
    }
}
