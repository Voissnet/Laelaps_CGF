package net.redvoiss.sms.smpp;

import org.smpp.Session;

/**
 *
 * @author Jorge Avila
 */
public interface SubmitTaskFactory {

    /**
     * Creates a submit task
     *
     * @param session
     * @param esmeEventHandler
     * @return
     */
    SubmitTask createSubmitTask(Session session, EsmeEventHandler esmeEventHandler);

}
