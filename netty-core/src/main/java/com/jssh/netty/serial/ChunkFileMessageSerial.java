package com.jssh.netty.serial;

import io.netty.buffer.ByteBuf;
import io.netty.handler.stream.ChunkedFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public abstract class ChunkFileMessageSerial implements FileMessageSerial {

    private final List<ChunkFile> serChunkFiles = new ArrayList<>();
    private final List<ChunkFile> deSerChunkFiles = new ArrayList<>();

    protected void addSerChunkFile(ChunkFile chunkedFile) {
        serChunkFiles.add(chunkedFile);
    }

    @Override
    public List<ChunkFile> getSerChunkFiles() {
        return serChunkFiles;
    }

    protected void clearSerChunkFiles() {
        serChunkFiles.clear();
    }

    protected void addDeSerChunkFile(ChunkFile chunkedFile) {
        deSerChunkFiles.add(chunkedFile);
    }

    @Override
    public List<ChunkFile> getDeSerChunkFiles() {
        return deSerChunkFiles;
    }

    protected void clearDeSerChunkFiles() {
        deSerChunkFiles.clear();
    }

    @Override
    public void writeSerChunkFiles(List<Object> out) throws IOException {
        List<ChunkFile> files = getSerChunkFiles();
        if (files != null && files.size() > 0) {
            for (ChunkFile f : files) {
                out.add(new ChunkedFile(f.getSrcFile()));
            }
        }
        clearSerChunkFiles();
    }

    @Override
    public boolean deSerializeComplete(Object builder, ByteBuf in) throws IOException {
        int len = in.readableBytes();
        List<ChunkFile> chunkFiles = getDeSerChunkFiles();
        if (chunkFiles == null || chunkFiles.isEmpty()) {
            return true;
        }
        if (len == 0) {
            return false;
        }
        for (Iterator<ChunkFile> it = chunkFiles.iterator(); it.hasNext(); ) {
            ChunkFile file = it.next();
            if (file.writeFrom(in)) {
                return false;
            }
            file.close();
            it.remove();
        }

        clearDeSerChunkFiles();
        return true;
    }

    protected ChunkFile createChunkFile(long length) throws IOException {
        File tempFile = creatTempFile(length);
        return new ChunkFile(tempFile, length);
    }

    protected File creatTempFile(long length) throws IOException {
        return File.createTempFile(UUID.randomUUID().toString(), ".file-cache");
    }
}
