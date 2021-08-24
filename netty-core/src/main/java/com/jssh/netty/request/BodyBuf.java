package com.jssh.netty.request;

import com.jssh.netty.serial.NettyFile;
import io.netty.buffer.ByteBuf;

import java.util.List;

public class BodyBuf {

    private ByteBuf bodyBuf;
    private int start;
    private int length;

    private List<NettyFile> bodyBufFiles;

    public BodyBuf(ByteBuf bodyBuf, int start, int length, List<NettyFile> bodyBufFiles) {
        this.bodyBuf = bodyBuf.retain();
        this.start = start;
        this.length = length;
        this.bodyBufFiles = bodyBufFiles;
    }

    public ByteBuf getBodyBuf() {
        return bodyBuf;
    }

    public void setBodyBuf(ByteBuf bodyBuf) {
        this.bodyBuf = bodyBuf;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public List<NettyFile> getBodyBufFiles() {
        return bodyBufFiles;
    }

    public void setBodyBufFiles(List<NettyFile> bodyBufFiles) {
        this.bodyBufFiles = bodyBufFiles;
    }

    @Override
    protected void finalize() throws Throwable {
        releaseBodyBuf();
        super.finalize();
    }

    private void releaseBodyBuf() {
        if (this.bodyBuf != null) {
            bodyBuf.release();
            this.bodyBuf = null;
        }
        this.bodyBufFiles = null;
    }
}
