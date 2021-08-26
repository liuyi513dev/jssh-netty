package jssh.netty.rpc.core.request;

public interface BufNettyRequest extends NettyRequest {

    BodyBuf getBodyBuf();
}
