package com.jssh.netty.serial;

import io.netty.buffer.ByteBuf;

public abstract class BytesMessageSerial extends ChunkFileMessageSerial implements MessageSerial {
	
	@Override
	public void serialize(ByteBuf buf, Object body) throws Exception {
		byte[] serialToBytes = serialToBytes(body);
		buf.writeBytes(serialToBytes);
	}

	@Override
	public Object deSerialize(ByteBuf body) throws Exception {
		byte[] array = body.array();
		return deSerial(array);
	}
	
	protected abstract byte[] serialToBytes(Object body);
	
	protected abstract Object deSerial(byte[] bytes);
}
