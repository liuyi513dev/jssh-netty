package jssh.netty.rpc.core.request;

public class BodyBufNettyRequest extends BaseNettyRequest implements BufNettyRequest {

    private final BodyBuf bodyBuf;

    public BodyBufNettyRequest(NettyRequest request, BodyBuf bodyBuf) {
        super(request);
        this.bodyBuf = bodyBuf;
    }

    @Override
    public void setHeaders(HeaderList headers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBody(Object body) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setResponseId(String responseId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NettyRequest cloneRequest() {
        return new OriginalNewNettyRequest(this, true);
    }

    @Override
    public BodyBuf getBodyBuf() {
        return bodyBuf;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    static class OriginalNewNettyRequest extends BaseNettyRequest implements BufNettyRequest {

        private boolean useOriginal;

        private BodyBufNettyRequest original;

        public OriginalNewNettyRequest(BodyBufNettyRequest original, boolean useOriginal) {
            super(original);
            setRequestId(null);
            setResponseId(null);
            this.useOriginal = useOriginal;
            this.original = original;
        }

        @Override
        public void setBody(Object body) {
            super.setBody(body);
            this.useOriginal = false;
            this.original = null;
        }

        @Override
        public NettyRequest cloneRequest() {
            return new OriginalNewNettyRequest(this.original, useOriginal);
        }

        @Override
        public String toString() {
            return "[useOriginal=" + useOriginal + ", " + super.toString() + "]";
        }

        @Override
        public BodyBuf getBodyBuf() {
            return useOriginal ? original.bodyBuf : null;
        }
    }
}
