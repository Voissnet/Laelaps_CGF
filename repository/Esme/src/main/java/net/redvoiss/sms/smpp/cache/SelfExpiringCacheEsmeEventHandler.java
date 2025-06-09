package net.redvoiss.sms.smpp.cache;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import net.redvoiss.sms.smpp.EsmeEventHandler;

/**
 *
 * @author Jorge Avila
 */
public interface SelfExpiringCacheEsmeEventHandler extends EsmeEventHandler {

    /**
     * Activates underlying caching mechanism
     *
     * @param pool used to process eviction
     * @return list of future tasks
     */
    List<Future<?>> activate(ExecutorService pool);
}
