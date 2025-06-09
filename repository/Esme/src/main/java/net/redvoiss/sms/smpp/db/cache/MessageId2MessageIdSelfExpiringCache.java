package net.redvoiss.sms.smpp.db.cache;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import net.redvoiss.sms.dao.DAO;
import net.redvoiss.sms.smpp.SubmitContext;
import net.redvoiss.sms.smpp.cache.SelfExpiringCache;
import net.redvoiss.sms.smpp.bean.Message.MessageId;
import net.redvoiss.sms.smpp.cache.SelfExpiringCacheException;
import net.redvoiss.sms.smpp.db.SQLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import java.util.logging.Logger;
/**
 *
 * @author Jorge Avila
 */
public class MessageId2MessageIdSelfExpiringCache extends SelfExpiringCache<String, MessageId> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageId2MessageIdSelfExpiringCache.class);
    
    //private static final Logger LOGGER = Logger.getLogger(MessageId2MessageIdSelfExpiringCache.class.getName());
    

    /**
     * Creates self expiring cache
     *
     * @param name
     */
    public MessageId2MessageIdSelfExpiringCache(String name) {
        super(name);
    }

    /**
     * Searches for object if missing
     *
     * @param key
     * @return MessageId from DB
     */
    @Override
    public MessageId get(String key) {
        return map.computeIfAbsent(key, (messageId) -> {
            MessageId ret = null;
            try (Connection conn = SQLUtil.getConnection()) {
                List<net.redvoiss.sms.bean.Message> aMessageList = DAO.findMessageWithoutSequence(conn, messageId);
                if (aMessageList.isEmpty()) {
                    LOGGER.warn("Handling delivery: message is unavailable for id {}", messageId);
                } else if (aMessageList.size() > 1) {
                    LOGGER.warn("Handling delivery: too many entries for message id {}", messageId);
                } else {
                    final net.redvoiss.sms.bean.Message passivatedMessage = aMessageList.get(0);
                    ret = new MessageId(passivatedMessage.getSmsCode(), passivatedMessage.getSmscGWId());
                    LOGGER.debug("Handling delivery: recovered message id {} for {}", ret, messageId);
                }
            } catch (SQLException sqle) {
                LOGGER.error("Unexpected DB exception", sqle);
            }
            return ret;
        });
    }

    @Override
    public MessageId put(String messageId, MessageId id) throws SelfExpiringCacheException {
        MessageId ret = super.put(messageId, id);
        SQLUtil.execute(() -> {
            try (Connection conn = SQLUtil.getConnection()) {
                final String primary = id.getPrimary();
                int q = DAO.sent(conn, messageId, primary);
                if (q > 0) {
                    LOGGER.trace("Updated {} records asociated with message id {}", q, primary);
                } else {
                    LOGGER.warn("Unable to mark message as sent for message id {}", primary);
                }
            } catch (SQLException sqle) {
                LOGGER.error("Unexpected DB exception", sqle);
            }
        });
        return ret;
    }

    @Override
    public MessageId put(SubmitContext submitContext, String key, MessageId value) throws SelfExpiringCacheException {
        throw new UnsupportedOperationException("Not supported.");
    }
}
