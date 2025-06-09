package net.redvoiss.sms.gateway.gsm.lyric;

import net.redvoiss.sms.SMSException;

import java.util.concurrent.TimeoutException;

public class LyricTimeoutException extends SMSException {

    private static final long serialVersionUID = 1L;
    
    public LyricTimeoutException( TimeoutException te ) {
        super( te );
    }
}