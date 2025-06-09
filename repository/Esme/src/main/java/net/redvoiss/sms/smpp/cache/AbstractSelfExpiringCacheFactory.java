package net.redvoiss.sms.smpp.cache;

import net.redvoiss.sms.smpp.bean.Message;
import net.redvoiss.sms.smpp.bean.Message.MessageId;

/**
 *
 * @author Jorge Avila
 */
public interface AbstractSelfExpiringCacheFactory {

    /**
     * Delegates self expiring cache creation based on the underlying
     * persistence layer involved
     *
     * @param name
     * @return self expiring cache instance
     */
    public SelfExpiringCache<String, MessageId> getMessageId2MessageIdSelfExpiringCache(String name);

    /**
     *
     * @param name
     * @return
     */
    public SelfExpiringCache<Integer, Message> getSequence2MessageSelfExpiringCache(String name);
}
