package net.redvoiss.sms.smpp;

import java.util.concurrent.ExecutorService;

/**
 *
 * @author Jorge Avila
 */
public interface EsmeEventHandlerFactory {

    /**
     *
     * @param pool
     * @return
     */
    EsmeEventHandler getEsmeEventHandler(ExecutorService pool);

}
