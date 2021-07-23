package com.jssh.netty.spring;

import com.jssh.netty.handler.ActionInvocation;
import com.jssh.netty.request.Action;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

                    List<String> actions = methodAction != null && methodAction.value().length > 0 ?
                            Arrays.asList(methodAction.value()) : Arrays.asList(method.getName());

                    if (classAction != null && classAction.value().length > 0) {
                        actions = actions.stream().flatMap(action -> Arrays.stream(classAction.value()).
                                map(ca -> ca + "." + action)).collect(Collectors.toList());
                    }

                    if (actionMapping == null) {
                        actionMapping = new HashMap<>(5);
                    }

                    for (String action : actions) {
                        actionMapping.put(action, new ActionInvocation(method, target));
                    }
                }
            }
        }
        return actionMapping;
    }
}
