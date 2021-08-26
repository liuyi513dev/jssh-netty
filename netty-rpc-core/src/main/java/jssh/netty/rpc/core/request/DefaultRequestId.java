package jssh.netty.rpc.core.request;

import java.util.UUID;

public class DefaultRequestId implements RequestId {

    @Override
    public String nextId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
