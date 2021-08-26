package jssh.netty.rpc.core.serial;

public class DefaultMessageSerialFactory implements MessageSerialFactory {

    @Override
    public MessageSerial createMessageSerial() {
        return new NettyRequestSerial();
    }
}
