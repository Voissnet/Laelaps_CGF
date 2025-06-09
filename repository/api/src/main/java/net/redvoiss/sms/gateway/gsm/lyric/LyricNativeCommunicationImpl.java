package net.redvoiss.sms.gateway.gsm.lyric;

import net.redvoiss.sms.SMSException;

import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static java.text.Normalizer.Form;

import static net.redvoiss.sms.gateway.gsm.lyric.GAO.API_VERSION;

public abstract class LyricNativeCommunicationImpl {
    private static Class<LyricNativeCommunicationImpl> s_clazz = LyricNativeCommunicationImpl.class; 
    protected static final Logger LOGGER = Logger.getLogger(s_clazz.getName(), s_clazz.getName());
    protected static final int WAIT_FOR = 2;
    private ExecutorService m_pool = Executors.newFixedThreadPool(2);
    private String m_baseUrl;
    
    public LyricNativeCommunicationImpl(String userInfo, String hostname, int port) {
        m_baseUrl = String.format("%s://%s@%s:%d/cgi-bin/exec?username=%s&password=%s&api_version=%s&", getProtocol(), userInfo, hostname, port, "lyric_api", "lyric_api", API_VERSION);
    }

    public LyricNativeCommunicationImpl(String username, String password, String hostname, int port) {
        m_baseUrl = String.format("%s://%s:%d/cgi-bin/sms_api?username=%s&password=%s&api_version=%s&", getProtocol(), hostname, port, username, password, API_VERSION);
    }

    public LyricNativeCommunicationImpl(String hostname, int port) {
        m_baseUrl = String.format("%s://%s:%d/cgi-bin/sms_api?username=%s&password=%s&api_version=%s&", getProtocol(), hostname, port, "lyric_api", "lyric_api", API_VERSION);
    }
    
    public Result resetQueue() throws SMSException, LyricTimeoutException {
        final String request = "cmd=api_reset_queue&sms_dir=out";
        Future<String> future = performRequest( getRequest(request) );
        LOGGER.log(FINE, "net.redvoiss.sms.gsm.gw.reset_queue");
        Result ret = null;
        try {
            ret = new ResultImpl( future.get( WAIT_FOR, TimeUnit.MINUTES) );
        } catch ( InterruptedException ie ) {
            LOGGER.log(WARNING, "Unexpected interruption", ie);
        } catch ( ExecutionException ee ) {
            LOGGER.log(WARNING, "Unexpected execution exception", ee);
            throw new SMSException( ee );
        } catch ( TimeoutException te ) {
            LOGGER.log(WARNING, String.format( "Unexpected timeout while performing the following request {%s}", request), te);
            throw new LyricTimeoutException( te );
        }
        return ret;
    }
    
    public Result deleteMessage( int smsCode ) throws SMSException, LyricTimeoutException {
        final String request = "cmd=api_sms_delete_by_id&sms_dir=out&id=" + smsCode;
        Future<String> future = performRequest( getRequest(request) );
        Result ret = null;
        try {
            String json = future.get( WAIT_FOR, TimeUnit.MINUTES);
            LOGGER.log(FINE, "net.redvoiss.sms.gsm.gw.delete_message", new Object[] {smsCode, json} );
            ret = new ResultImpl( json );
        } catch ( InterruptedException ie ) {
            LOGGER.log(WARNING, "Unexpected interruption", ie);
        } catch ( ExecutionException ee ) {
            LOGGER.log(WARNING, "Unexpected execution exception", ee);
            throw new SMSException( ee );
        } catch ( TimeoutException te ) {
            LOGGER.log(WARNING, String.format( "Unexpected timeout while performing the following request {%s}", request), te);
            throw new LyricTimeoutException( te );
        }
        return ret;
    }
    
    public Result deleteReadMessages() throws SMSException, LyricTimeoutException {
        final String request = "cmd=api_sms_delete_by_status&sms_dir=in&status=read";
        Future<String> future = performRequest( getRequest(request) );
        Result ret = null;
        try {
            String json = future.get( WAIT_FOR, TimeUnit.MINUTES);
            LOGGER.log(FINE, "net.redvoiss.sms.gsm.gw.delete_read_messages", new Object[] {json} );
            ret = new ResultImpl( json );
        } catch ( InterruptedException ie ) {
            LOGGER.log(WARNING, "Unexpected interruption", ie);
        } catch ( ExecutionException ee ) {
            LOGGER.log(WARNING, "Unexpected execution exception", ee);
            throw new SMSException( ee );
        } catch ( TimeoutException te ) {
            LOGGER.log(WARNING, String.format( "Unexpected timeout while performing the following request {%s}", request), te);
            throw new LyricTimeoutException( te );
        }
        return ret;
    }
    
    public ReceiveMessageResult receiveMessage() throws SMSException, LyricTimeoutException {
        final String request = "cmd=api_recv_get_first_n_unread&set_read=1&n_regs=1";
        Future<String> future = performRequest( getRequest(request) );
        ReceiveMessageResult ret = null;
        try {
            String json = future.get( WAIT_FOR, TimeUnit.MINUTES);
            LOGGER.log(FINE, "net.redvoiss.sms.gsm.gw.receive_message", new Object[] {json} );
            ret = new ReceiveMessageResultImpl( json );;
        } catch ( InterruptedException ie ) {
            LOGGER.log(WARNING, "Unexpected interruption", ie);
        } catch ( ExecutionException ee ) {
            LOGGER.log(WARNING, "Unexpected execution exception", ee);
            throw new SMSException( ee );
        } catch ( TimeoutException te ) {
            LOGGER.log(WARNING, String.format( "Unexpected timeout while performing the following request {%s}", request), te);
            throw new LyricTimeoutException( te );
        }
        return ret;
    }
    
    public SendMessageResult sendMessage( String destination, String message)  throws SMSException, LyricTimeoutException {
        SendMessageResult ret = null;
        try { 
            final String msg = URLEncoder.encode( Normalizer.normalize( message, Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", ""), "UTF-8" );
            final String request = String.format("cmd=api_queue_sms&destination=%s&content=%s", destination, msg);
            Future<String> future = performRequest( getRequest(request) );
            try {
                String json = future.get( WAIT_FOR, TimeUnit.MINUTES);
                LOGGER.log(FINE, "net.redvoiss.sms.gsm.gw.send_message", new Object[] {json} );
                ret = new SendMessageResultImpl( json );
            } catch ( InterruptedException ie ) {
                LOGGER.log(WARNING, "Unexpected interruption", ie);
            } catch ( ExecutionException ee ) {
                LOGGER.log(WARNING, "Unexpected execution exception", ee);
                throw new SMSException( ee );
            } catch ( TimeoutException te ) {
                LOGGER.log(WARNING, String.format( "Unexpected timeout while performing the following request {%s}", request), te);
                throw new LyricTimeoutException( te );
            }
        } catch ( java.io.UnsupportedEncodingException e ) {
            LOGGER.log( SEVERE, "Unsupported encoding", e);
            throw new SMSException( e);
        }
        return ret;
    }
    
    public ChannelStatusResult getChannelStatus() throws SMSException, LyricTimeoutException {
        final String request = "cmd=api_get_channels_status";
        Future<String> future = performRequest( getRequest(request) );
        ChannelStatusResult ret = null;
        try {
            final String json = future.get( WAIT_FOR, TimeUnit.MINUTES);
            LOGGER.log(FINE, "net.redvoiss.sms.gsm.gw.get_channel_status", new Object[] {json} );
            ret = new ChannelStatusResultImpl( json );
        } catch ( InterruptedException ie ) {
            LOGGER.log(WARNING, "Unexpected interruption", ie);
        } catch ( ExecutionException ee ) {
            LOGGER.log(WARNING, "Unexpected execution exception", ee);
            throw new SMSException( ee );
        } catch ( TimeoutException te ) {
            LOGGER.log(WARNING, String.format( "Unexpected timeout while performing the following request {%s}", request), te);
            throw new LyricTimeoutException( te );
        }
        return ret;
    }
    
    public QueueStatusResult getQueueStatus() throws SMSException, LyricTimeoutException {
        final String request = "cmd=api_get_queue_status";
        Future<String> future = performRequest( getRequest(request) );
        QueueStatusResult ret = null;
        try {
            String json = future.get( WAIT_FOR, TimeUnit.MINUTES);
            ret = new QueueStatusResultImpl( json );
        } catch ( InterruptedException ie ) {
            LOGGER.log(WARNING, "Unexpected interruption", ie);
        } catch ( ExecutionException ee ) {
            LOGGER.log(WARNING, "Unexpected execution exception", ee);
            throw new SMSException( ee );
        } catch ( TimeoutException te ) {
            LOGGER.log(WARNING, String.format( "Unexpected timeout while performing the following request {%s}", request), te);
            throw new LyricTimeoutException( te );
        }
        return ret;
    }
    
    public MessageStatusResult getMessageStatus( int smsCode ) throws SMSException, LyricTimeoutException {
        final String request = "cmd=api_get_status&message_id=" + smsCode;
        Future<String> future = performRequest( getRequest(request) );
        MessageStatusResult ret = null;
        try {
            String json = future.get( WAIT_FOR, TimeUnit.MINUTES);
            LOGGER.log(FINE, "net.redvoiss.sms.gsm.gw.message_status", new Object[] {smsCode, json} );
            ret = new MessageStatusResultImpl( json );;
        } catch ( InterruptedException ie ) {
            LOGGER.log(WARNING, "Unexpected interruption", ie);
        } catch ( ExecutionException ee ) {
            LOGGER.log(WARNING, "Unexpected execution exception", ee);
            throw new SMSException( ee );
        } catch ( TimeoutException te ) {
            LOGGER.log(WARNING, String.format( "Unexpected timeout while performing the following request {%s}", request), te);
            throw new LyricTimeoutException( te );
        }
        return ret;
    }

    public VersionResult getVersion() throws SMSException, LyricTimeoutException {
        final String request = "cmd=api_get_version";
        Future<String> future = performRequest( getRequest(request) );
        VersionResult ret = null;
        try {
            String json = future.get( WAIT_FOR, TimeUnit.MINUTES);
            ret = new VersionResultImpl( json );
        } catch ( InterruptedException ie ) {
            LOGGER.log(WARNING, "Unexpected interruption", ie);
        } catch ( ExecutionException ee ) {
            LOGGER.log(WARNING, "Unexpected execution exception", ee);
            throw new SMSException( ee );
        } catch ( TimeoutException te ) {
            LOGGER.log(WARNING, String.format( "Unexpected timeout while performing the following request {%s}", request), te);
            throw new LyricTimeoutException( te );
        }
        return ret;
    }
    
    abstract String getProtocol();
    
    private String getRequest(final String cmd) {
        return m_baseUrl + cmd;
    }
        
    private Future<String> performRequest( String url ) throws SMSException {
        return m_pool.submit( getTask( url) );
    }
    
    abstract Callable<String> getTask( String url );
 
}