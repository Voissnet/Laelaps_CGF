package net.redvoiss.sms.smpp.db.cache;

import java.sql.Connection;
import java.sql.SQLException;
import net.redvoiss.sms.dao.DAO;
import net.redvoiss.sms.smpp.SubmitContext;
import net.redvoiss.sms.smpp.bean.Message;
import net.redvoiss.sms.smpp.cache.SelfExpiringCache;
import net.redvoiss.sms.smpp.cache.SelfExpiringCacheException;
import net.redvoiss.sms.smpp.db.SQLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import java.util.logging.Logger;

/**
 *
 * @author Jorge Avila
 */
public class Sequence2MessageSelfExpiringCache extends SelfExpiringCache<Integer, Message> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Sequence2MessageSelfExpiringCache.class);
    //private static final java.util.logging.Logger LOGGER = Logger.getLogger(Sequence2MessageSelfExpiringCache.class.getName());

    /**
     * Creates self expiring cache
     *
     * @param name
     */
    public Sequence2MessageSelfExpiringCache(String name) {
        super(name);
    }

    /**
     *
     * @param submitContext
     * @param sequence
     * @param message
     * @return
     * @throws SelfExpiringCacheException
     */
    @Override
    public Message put(SubmitContext submitContext, Integer sequence, Message message) throws SelfExpiringCacheException {
        Message ret = super.put(sequence, message);
        /* Puts it into the execution queue */
        SQLUtil.execute(() -> {
            try {// DO NOT USE 'try + resources'
                Connection conn = (Connection) submitContext.getWrappedObject();// This will be close latter in time
                final String primary = message.getId().getPrimary();
                int q = DAO.transmitted(conn, primary);
                if (q < 1) {
                    LOGGER.warn("Unable to mark message id {} as transmitted", primary);
                }
            } catch (SQLException sqle) {
                LOGGER.error("Unexpected DB exception", sqle);
            }
        });
        return ret;
    }

}
