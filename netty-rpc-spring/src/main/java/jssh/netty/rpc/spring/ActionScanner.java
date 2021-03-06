package jssh.netty.rpc.spring;

import jssh.netty.rpc.core.AbstractNettyManager;
import jssh.netty.rpc.core.handler.ActionInvocation;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Map;

public class ActionScanner implements BeanPostProcessor {

    private final ActionScannerProcessor processor = new ActionScannerProcessor();

    private final AbstractNettyManager nettyManager;

    public ActionScanner(AbstractNettyManager nettyManager) {
        this.nettyManager = nettyManager;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Map<String, ActionInvocation> actions = processor.scan(bean);
        if (actions != null) {
            nettyManager.getRequestHandler().addAction(actions);
        }
        return bean;
    }
}
