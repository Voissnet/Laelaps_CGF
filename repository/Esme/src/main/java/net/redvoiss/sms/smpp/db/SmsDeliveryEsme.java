package net.redvoiss.sms.smpp.db;

import java.io.IOException;
import java.util.function.Predicate;
import net.redvoiss.sms.smpp.EsmeEventHandler;
import net.redvoiss.sms.smpp.cache.SelfExpiringCacheEsmeEventHandler;
import net.redvoiss.sms.smpp.SubmitTaskFactory;
import net.redvoiss.sms.smpp.bean.Message;
import net.redvoiss.sms.smpp.db.impl.DBSelfExpiringCacheEsmeEventHandlerImpl;
import net.redvoiss.sms.smpp.db.impl.AbstractSubmitTaskImpl;
import net.redvoiss.sms.smpp.db.cache.SelfExpiringCacheFactoryImpl;
import net.redvoiss.sms.smpp.dlr.impl.TeltechDlrParser;
import org.smpp.Session;
import org.smpp.SmppException;

/**
 *
 * @author Jorge Avila
 */
public class SmsDeliveryEsme extends AbstractDBEsme {

    /**
     * Runs default setting
     *
     * @param args
     * @throws SmppException
     * @throws IOException
     */
    public static void main(String args[]) throws SmppException, IOException {
        new SmsDeliveryEsme(
                new DBSelfExpiringCacheEsmeEventHandlerImpl(new SelfExpiringCacheFactoryImpl(),
                        (String dlr) -> new TeltechDlrParser(dlr)),
                (Session session, EsmeEventHandler esmeEventHandler) -> {
                    return new AbstractSubmitTaskImpl(session, esmeEventHandler) {
                @Override
                public Predicate<Message> getFilter() {
                    return (Message message) -> true;
                }
            };
                }).run();
    }

    /**
     * Defines client constructor
     *
     * @param esmeEventHandler
     * @param submitTaskAbstractFactory
     * @throws SmppException
     * @throws IOException
     */
    public SmsDeliveryEsme(SelfExpiringCacheEsmeEventHandler esmeEventHandler, SubmitTaskFactory submitTaskAbstractFactory) throws SmppException, IOException {
        super(esmeEventHandler, submitTaskAbstractFactory);
    }
}
