package jssh.netty.rpc.core;

public class Configuration {

    private int executorSize = 10;
    private int bossCount = 5;
    private int workerCount = 10;
    private int responseTimeoutMillis = 600000;
    private int readerIdleMillis = 180000;
    private int writerIdleMillis = 120000;
    private int allIdleMillis = 240000;
    private int writeTimeoutMillis = 600000;
    private int backlog = 128;
    private int sndbuf = 32768;
    private int rcvbuf = 32768;
    private boolean keepAlive = true;
    private int connectTimeoutMillis = 10000;
    private int maxFrameLength = 2048 * 1024;
    private boolean printMessage = true;
    private boolean logging = false;

    private SSL ssl = new SSL();

    public int getExecutorSize() {
        return executorSize;
    }

    public void setExecutorSize(int executorSize) {
        this.executorSize = executorSize;
    }

    public int getBossCount() {
        return bossCount;
    }

    public void setBossCount(int bossCount) {
        this.bossCount = bossCount;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

    public int getResponseTimeoutMillis() {
        return responseTimeoutMillis;
    }

    public void setResponseTimeoutMillis(int responseTimeoutMillis) {
        this.responseTimeoutMillis = responseTimeoutMillis;
    }

    public int getReaderIdleMillis() {
        return readerIdleMillis;
    }

    public void setReaderIdleMillis(int readerIdleMillis) {
        this.readerIdleMillis = readerIdleMillis;
    }

    public int getWriterIdleMillis() {
        return writerIdleMillis;
    }

    public void setWriterIdleMillis(int writerIdleMillis) {
        this.writerIdleMillis = writerIdleMillis;
    }

    public int getAllIdleMillis() {
        return allIdleMillis;
    }

    public void setAllIdleMillis(int allIdleMillis) {
        this.allIdleMillis = allIdleMillis;
    }

    public int getWriteTimeoutMillis() {
        return writeTimeoutMillis;
    }

    public void setWriteTimeoutMillis(int writeTimeoutMillis) {
        this.writeTimeoutMillis = writeTimeoutMillis;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public int getSndbuf() {
        return sndbuf;
    }

    public void setSndbuf(int sndbuf) {
        this.sndbuf = sndbuf;
    }

    public int getRcvbuf() {
        return rcvbuf;
    }

    public void setRcvbuf(int rcvbuf) {
        this.rcvbuf = rcvbuf;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getMaxFrameLength() {
        return maxFrameLength;
    }

    public void setMaxFrameLength(int maxFrameLength) {
        this.maxFrameLength = maxFrameLength;
    }

    public boolean isPrintMessage() {
        return printMessage;
    }

    public void setPrintMessage(boolean printMessage) {
        this.printMessage = printMessage;
    }

    public boolean isLogging() {
        return logging;
    }

    public void setLogging(boolean logging) {
        this.logging = logging;
    }

    public SSL getSsl() {
        return ssl;
    }

    public void setSsl(SSL ssl) {
        this.ssl = ssl;
    }

    public static class SSL {
        private boolean enable;
        private String keyStorePath;
        private String keyStorePassword;
        private String keyPassword;
        private String trustCertificatePath;
        private String trustCertificateAlias;
        private String protocol;
        private boolean clientMode;
        private boolean needClientAuth;

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public String getKeyStorePath() {
            return keyStorePath;
        }

        public void setKeyStorePath(String keyStorePath) {
            this.keyStorePath = keyStorePath;
        }

        public String getKeyStorePassword() {
            return keyStorePassword;
        }

        public void setKeyStorePassword(String keyStorePassword) {
            this.keyStorePassword = keyStorePassword;
        }

        public String getTrustCertificatePath() {
            return trustCertificatePath;
        }

        public void setTrustCertificatePath(String trustCertificatePath) {
            this.trustCertificatePath = trustCertificatePath;
        }

        public String getTrustCertificateAlias() {
            return trustCertificateAlias;
        }

        public void setTrustCertificateAlias(String trustCertificateAlias) {
            this.trustCertificateAlias = trustCertificateAlias;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public boolean isClientMode() {
            return clientMode;
        }

        public void setClientMode(boolean clientMode) {
            this.clientMode = clientMode;
        }

        public boolean isNeedClientAuth() {
            return needClientAuth;
        }

        public void setNeedClientAuth(boolean needClientAuth) {
            this.needClientAuth = needClientAuth;
        }

        public String getKeyPassword() {
            return keyPassword;
        }

        public void setKeyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
        }
    }
}
