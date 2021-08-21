package com.jssh.netty.serial;

import io.netty.buffer.ByteBuf;
import io.netty.handler.stream.ChunkedFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public interface FileMessageSerial extends MessageSerial {

	List<ChunkFile> getSerChunkFiles();

	List<ChunkFile> getDeSerChunkFiles();

	void writeSerChunkFiles(List<Object> out) throws IOException;

	boolean deSerializeComplete(Object body, ByteBuf in) throws IOException;
}
