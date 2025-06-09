package net.redvoiss.sms.smpp;

import net.redvoiss.sms.smpp.bean.Message;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 *
 * @author Jorge Avila
 */
public interface SubmitTask extends Runnable {

    /**
     *
     * @param wrappedObject
     * @return
     */
    Stream<Message> getMessageStream(SubmitContext wrappedObject);

    /**
     *
     * @return
     */
    Predicate<Message> getFilter();
}
