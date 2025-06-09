package net.redvoiss.sms.smpp;

/**
 *
 * @author Jorge Avila
 * @param <T>
 */
public interface SubmitContext<T> {

    /**
     * Returns wrapped object
     *
     * @return
     */
    T getWrappedObject();
}
