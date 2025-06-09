package net.redvoiss.sms.smpp.cache;

/**
 *
 * @author Jorge Avila
 */
public class SelfExpiringCacheException extends Exception {

    /**
     *
     * @param cause
     */
    public SelfExpiringCacheException(Throwable cause) {
        super(cause);
    }

    /**
     * Default constructor
     */
    public SelfExpiringCacheException() {
    }

}
