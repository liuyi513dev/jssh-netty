package jssh.netty.rpc.core.handler;

public class ReturnValue {

    private boolean isReturn;

    private Object returnValue;

    public ReturnValue(boolean isReturn, Object returnValue) {
        super();
        this.isReturn = isReturn;
        this.returnValue = returnValue;
    }

    public boolean isReturn() {
        return isReturn;
    }

    public void setReturn(boolean isReturn) {
        this.isReturn = isReturn;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }
}
