package net.redvoiss.sms.gateway.gsm.lyric;

import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;

public class LyricNativePlainCommunicationImpl extends LyricNativeCommunicationImpl implements GAO {

    public LyricNativePlainCommunicationImpl(String userInfo, String hostname, int port) {
        super(userInfo, hostname, port);
    }

    public LyricNativePlainCommunicationImpl(String username, String password, String hostname, int port) {
        super(username, password, hostname, port);
    }
    
    public LyricNativePlainCommunicationImpl(String hostname, int port) {
		super(hostname, port);
    }
    
    Callable<String> getTask( String url ) {
        return new Task( url );
    }

    class Task implements Callable<String> {
        String url;

        Task( String u ) {
            url = u;
        }

        public String call() throws IOException, MalformedURLException {
            String ret = null;
            URL aURL = new URL(url);
            URLConnection aURLConnection = aURL.openConnection();
            synchronized( aURLConnection ) {
                String userInfo = aURL.getUserInfo();
                if ( userInfo == null || userInfo.isEmpty() ) {
                } else {
                    String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary( userInfo.getBytes() );
                    aURLConnection.setRequestProperty("Authorization", basicAuth);
                }
                try ( InputStream is = aURLConnection.getInputStream() ) {
                    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                    ret = (s.hasNext() ? s.next() : "");
                }
            }
            return ret;
        }
    }
    
    protected String getProtocol() {
        return "http";
    }
}