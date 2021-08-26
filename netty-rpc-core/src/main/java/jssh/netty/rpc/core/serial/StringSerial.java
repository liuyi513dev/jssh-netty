package jssh.netty.rpc.core.serial;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

public class StringSerial implements MessageSerial {

    @Override
    public void serialize(ByteBuf buf, Object object) throws Exception {
        Objects.requireNonNull(object);
        SerialUtils.writeLengthCharSequence(buf, parseString(object));
    }

    @Override
    public Object deSerialize(ByteBuf in) throws Exception {
        return parseObject(SerialUtils.readCharSequence(in));
    }

    public String parseString(Object object) {
        return (String) object;
    }

    public Object parseObject(String string) {
        return string;
    }
}
