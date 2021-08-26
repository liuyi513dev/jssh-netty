package jssh.netty.rpc.core.server;

import io.netty.channel.ChannelHandlerContext;
import jssh.netty.rpc.core.exception.ValidationException;

public interface ClientValidator {

    ClientInfo<?> validate(ChannelHandlerContext ctx, Object param) throws ValidationException;
}
