package net.redvoiss.sms.gateway.gsm.lyric;

import javax.net.ssl.TrustManager;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

public class LyricNativeSSLCommunicationImpl extends LyricNativeCommunicationImpl implements GAO {
    
    public LyricNativeSSLCommunicationImpl(String username, String password, String hostname, int port) {
        super(username, password, hostname, port);
    }
    
    public LyricNativeSSLCommunicationImpl(String hostname, int port) {
		super(hostname, port);
    }
    
    protected static final TrustManager[] TRUST_ALL_CERTS_TRUSTMANAGER = new TrustManager[] { new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() { return null; }
        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
        public void checkServerTrusted(X509Certificate[] certs, String authType) { }

    } };

    Callable<String> getTask( String url ) {
        return new Task( url );
    }
    
    class Task implements Callable<String> {
        String url;

        Task( String u ) {
            url = u;
        }

        public String call() throws IOException, MalformedURLException, NoSuchAlgorithmException, KeyManagementException {
            String ret = null;
            HttpsURLConnection aURLConnection = (HttpsURLConnection) new URL(url).openConnection();
            synchronized( aURLConnection ) {
                aURLConnection.setHostnameVerifier((hostname, session) -> true);
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, TRUST_ALL_CERTS_TRUSTMANAGER, new SecureRandom());
                aURLConnection.setSSLSocketFactory(sc.getSocketFactory());            
                try ( InputStream is = aURLConnection.getInputStream() ) {
                    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                    ret = (s.hasNext() ? s.next() : "");
                } finally {

                }
            }
            return ret;
        }
    }
    
    protected String getProtocol() {
        return "https";
    }
}