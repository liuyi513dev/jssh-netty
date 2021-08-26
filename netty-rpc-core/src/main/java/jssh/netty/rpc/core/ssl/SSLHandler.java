package jssh.netty.rpc.core.ssl;

import jssh.netty.rpc.core.Configuration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

public class SSLHandler {

    private Configuration.SSL ssl;

    private SSLContext sslContext;

    public void initSSL(Configuration.SSL ssl) throws Exception {
        this.ssl = ssl;
        if (ssl.isEnable() && sslContext == null) {
            initSSLContext();
        }
    }

    private void initSSLContext() throws KeyStoreException, IOException,
            NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException, KeyManagementException {

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        try (InputStream in = loadInputStream(ssl.getKeyStorePath())) {
            keyStore.load(in, ssl.getKeyStorePassword().toCharArray());
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, ssl.getKeyPassword().toCharArray());

        KeyStore trustCertificateStore = keyStore;
        if (ssl.getTrustCertificatePath() != null) {

            trustCertificateStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustCertificateStore.load(null, null);

            try (InputStream in = loadInputStream(ssl.getTrustCertificatePath())) {
                trustCertificateStore.setCertificateEntry(ssl.getTrustCertificateAlias(),
                        CertificateFactory.getInstance("X.509").generateCertificate(in));
            }
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(trustCertificateStore);

        this.sslContext = SSLContext.getInstance(ssl.getProtocol());
        this.sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
    }

    private InputStream loadInputStream(String filePath) throws FileNotFoundException {
        InputStream stream = this.getClass().getResourceAsStream(filePath);
        if (stream == null && new File(filePath).exists()) {
            stream = new FileInputStream(filePath);
        }
        return stream;
    }

    public SSLEngine createSSLEngine() {
        if (sslContext == null) {
            return null;
        }
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(ssl.isClientMode());
        sslEngine.setNeedClientAuth(ssl.isNeedClientAuth());
        return sslEngine;
    }

    public Configuration.SSL getSsl() {
        return ssl;
    }

    public void setSsl(Configuration.SSL ssl) {
        this.ssl = ssl;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }
}
