package net.redvoiss.sms;

public class SMSException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    public SMSException( Throwable t ) {
        super( t );
    }
    public SMSException (String message) {
        super(message);
    }
}