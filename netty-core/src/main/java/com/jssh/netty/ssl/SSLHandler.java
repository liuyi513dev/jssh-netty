package com.jssh.netty.ssl;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

public class SSLHandler {

    private boolean enable;

    private String keyStorePath;
    private String keyStorePassword;
    private String trustCertificatePath;
    private String trustCertificateAlias;
    private String protocol;
    private boolean clientMode;
    private boolean needClientAuth;

    private SSLContext sslContext;

    public void initSSL() throws Exception {
        if (enable && sslContext == null) {
            initSSLContext();
        }
    }

    private void initSSLContext() throws KeyStoreException, FileNotFoundException, IOException,
            NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException, KeyManagementException {

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream keyStoreStream = this.getClass().getResourceAsStream(keyStorePath);
        if (keyStoreStream == null && new File(keyStorePath).exists()) {
            keyStoreStream = new FileInputStream(keyStorePath);
        }
        try (InputStream in = keyStoreStream) {
            keyStore.load(in, keyStorePassword.toCharArray());
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

        KeyStore trustCertificateStore = keyStore;
        if (trustCertificatePath != null) {

            trustCertificateStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustCertificateStore.load(null, null);

            InputStream trustCertificateStream = this.getClass().getResourceAsStream(trustCertificatePath);
            if (trustCertificateStream == null && new File(trustCertificatePath).exists()) {
                trustCertificateStream = new FileInputStream(trustCertificatePath);
            }
            try (InputStream in = trustCertificateStream) {
                trustCertificateStore.setCertificateEntry(trustCertificateAlias,
                        CertificateFactory.getInstance("X.509").generateCertificate(in));
            }
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(trustCertificateStore);

        this.sslContext = SSLContext.getInstance(protocol);
        this.sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
    }

    public SSLEngine createSSLEngine() {
        if (!enable || sslContext == null) {
            return null;
        }
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(clientMode);
        sslEngine.setNeedClientAuth(needClientAuth);
        return sslEngine;
    }

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

    public SSLContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
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
}
