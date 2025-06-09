package net.redvoiss.sms.smpp.db;

import java.io.IOException;
import net.redvoiss.sms.smpp.AbstractEsme;
import net.redvoiss.sms.smpp.SubmitTaskFactory;
import net.redvoiss.sms.smpp.cache.SelfExpiringCacheEsmeEventHandler;
import org.smpp.SmppException;

/**
 *
 * @author Jorge Avila
 */
public abstract class AbstractDBEsme extends AbstractEsme {

    /**
     * Route
     */
    public final static int ROUTE = Integer.parseInt(System.getProperty("esme.route", "-1"));

    /**
     *
     * @param esmeEventHandler
     * @param submitTaskAbstractFactory
     * @throws SmppException
     * @throws IOException
     */
    public AbstractDBEsme(SelfExpiringCacheEsmeEventHandler esmeEventHandler, SubmitTaskFactory submitTaskAbstractFactory) throws SmppException, IOException {
        super(esmeEventHandler, submitTaskAbstractFactory);
    }

    /**
     *
     */
    @Override
    public void cleanUp() {
        super.cleanUp();
        shutdown(SQLUtil.getExecutionPool());
    }

}
