package jssh.netty.rpc.core.serial;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class SerialUtils {

    public static final Charset default_charset = StandardCharsets.UTF_8;

    public static void writeLengthCharSequence(ByteBuf buf, CharSequence seq) {
        int lenIndex = buf.writerIndex();
        buf.writeInt(0);
        int len = buf.writeCharSequence(seq, default_charset);
        buf.markWriterIndex();
        buf.setInt(lenIndex, len);
        buf.resetWriterIndex();
    }

    public static String readCharSequence(ByteBuf buf) {
        return buf.readCharSequence(buf.readInt(), default_charset).toString();
    }
}
