package net.redvoiss.sms.services.error;

import javax.xml.ws.WebFault;
/**
* @see http://blog.coffeebeans.at/archives/73
*/
@WebFault
public class SMSError extends Exception {
    public SMSError() {
        super();
    }
    public SMSError(final Throwable thr) {
        super(thr);
    }
    public SMSError(final String msg) {
        super(msg);
    }
}