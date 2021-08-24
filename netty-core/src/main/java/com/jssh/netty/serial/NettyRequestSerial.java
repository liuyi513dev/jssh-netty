package com.jssh.netty.serial;

import com.jssh.netty.request.BodyBufRequestBuilder;
import com.jssh.netty.request.BufNettyRequest;
import com.jssh.netty.request.HeaderList;
import com.jssh.netty.request.NettyRequest;
import io.netty.buffer.ByteBuf;
import io.netty.handler.stream.ChunkedFile;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class NettyRequestSerial extends DefaultSerial {

    private final boolean isBodyBuf;

    public NettyRequestSerial() {
        this(true);
    }

    public NettyRequestSerial(boolean isBodyBuf) {
        this.isBodyBuf = isBodyBuf;
        addInnerSerial(NettyFile.class, TYPE.NETTY_FILE, new NettyFileSerial());
    }

    private CurrentNettyRequestHolder currentNettyRequestHolder;

    private final LinkedList<NettyFile> serialNettyFiles = new LinkedList<>();

    private final LinkedList<NettyFile> deSerialNettyFiles = new LinkedList<>();

    public LinkedList<NettyFile> getSerialNettyFiles() {
        return serialNettyFiles;
    }

    public LinkedList<NettyFile> getDeSerialNettyFiles() {
        return deSerialNettyFiles;
    }

    @Override
    public synchronized void serialize(ByteBuf buf, Object object, List<Object> out) throws Exception {
        NettyRequest request = (NettyRequest) object;

        if (request == null) {
            throw new Exception("The encode message is null");
        }

        serialize(buf, request.getSyn());
        serialize(buf, request.getAck());
        serialize(buf, request.getRequired());
        serialize(buf, request.getRequestAction());
        serialize(buf, request.getRequestId());
        serialize(buf, request.getResponseId());
        serialize(buf, request.getHeaders());

        if (isBodyBuf && request instanceof BufNettyRequest) {

            BodyBuf bodyBuf = ((BufNettyRequest) request).getBodyBuf();
            synchronized (bodyBuf) {
                ByteBuf body = bodyBuf.getBodyBuf();
                body.markReaderIndex();
                buf.writeBytes(body, bodyBuf.getStart(), bodyBuf.getLength());
                body.resetReaderIndex();
            }
            List<NettyFile> bodyBufFiles = bodyBuf.getBodyBufFiles();
            if (bodyBufFiles != null && bodyBufFiles.size() > 0) {
                getSerialNettyFiles().addAll(bodyBufFiles);
            }
        } else {
            serialize(buf, request.getBody());
        }

        for (NettyFile f : getSerialNettyFiles()) {
            out.add(new ChunkedFile(f.getSrcFile()));
        }

        getSerialNettyFiles().clear();
    }

    @Override
    public synchronized Object deSerialize(ByteBuf in) throws Exception {

        if (currentNettyRequestHolder != null) {
            if (deSerialNettyFilesWriteComplete(in)) {
                NettyRequest request = currentNettyRequestHolder.getBuilder().build();
                currentNettyRequestHolder = null;
                return request;
            }
            return null;
        }

        BodyBufRequestBuilder builder = BodyBufRequestBuilder.builder();

        builder.setSyn((Boolean) readObject(in));
        builder.setAck((Boolean) readObject(in));
        builder.setRequired((Boolean) readObject(in));
        builder.setRequestAction((String) readObject(in));
        builder.setRequestId((String) readObject(in));
        builder.setResponseId((String) readObject(in));
        builder.setHeaders((HeaderList) readObject(in));

        int start = in.readerIndex();
        Object body = readObject(in);
        builder.setBody(body);
        int end = in.readerIndex();

        List<NettyFile> nettyFiles = getDeSerialNettyFiles();

        if (isBodyBuf && body != null && end > start) {
            // message.setBodyBuf(in.copy(start, end - start));
            List<NettyFile> bodyBufFiles = null;
            if (!nettyFiles.isEmpty()) {
                bodyBufFiles = new LinkedList<>(nettyFiles);
            }
            BodyBuf bodyBuf = new BodyBuf(in, start, end - start, bodyBufFiles);
            builder.setBodyBuf(bodyBuf);
        }

        if (nettyFiles.isEmpty()) {
            return builder.build();
        }

        currentNettyRequestHolder = new CurrentNettyRequestHolder(builder);
        return null;
    }

    public boolean deSerialNettyFilesWriteComplete(ByteBuf in) throws IOException {
        int len = in.readableBytes();
        if (getDeSerialNettyFiles().isEmpty()) {
            return true;
        }
        if (len == 0) {
            return false;
        }
        for (Iterator<NettyFile> it = getDeSerialNettyFiles().iterator(); it.hasNext(); ) {
            NettyFile file = it.next();
            if (file.writeFrom(in)) {
                return false;
            }
            file.close();
            it.remove();
        }
        return true;
    }

    static class CurrentNettyRequestHolder {

        private final BodyBufRequestBuilder builder;

        public CurrentNettyRequestHolder(BodyBufRequestBuilder builder) {
            this.builder = builder;
        }

        public BodyBufRequestBuilder getBuilder() {
            return builder;
        }
    }

    class NettyFileSerial implements SerialHandler.InnerSerial {

        @Override
        public void write(ByteBuf buf, Object obj) {
            buf.writeLong(((NettyFile) obj).getLength());
            getSerialNettyFiles().add((NettyFile) obj);
        }

        @Override
        public Object read(ByteBuf buf) throws IOException {
            long length = buf.readLong();
            File tempFile = File.createTempFile(UUID.randomUUID().toString(), ".file-cache");
            NettyFile nettyFile = new NettyFile(tempFile, length);
            getDeSerialNettyFiles().add(nettyFile);
            return nettyFile;
        }
    }
}
