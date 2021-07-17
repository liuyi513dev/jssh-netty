package com.jssh.netty.serial;

import java.io.IOException;
import java.util.List;

import io.netty.buffer.ByteBuf;

public interface MessageSerial {
	
	boolean support(Object body);
	
	void serialize(ByteBuf buf, Object body) throws Exception;
	
	void afterSerialize(List<Object> out) throws IOException;
	
	Object deSerialize(ByteBuf body) throws Exception;
	
	List<ChunkFile> getSerChunkFiles();
	
	List<ChunkFile> getDeSerChunkFiles();

	boolean deSerializeComplete(Object body, ByteBuf in) throws IOException;
}
