package net.redvoiss.sms.smpp.db.cache;

import net.redvoiss.sms.smpp.cache.AbstractSelfExpiringCacheFactory;
import net.redvoiss.sms.smpp.cache.SelfExpiringCache;
import net.redvoiss.sms.smpp.bean.Message.MessageId;

/**
 *
 * @author Jorge Avila
 */
public class SelfExpiringCacheFactoryImpl implements AbstractSelfExpiringCacheFactory {

    @Override
    public SelfExpiringCache<String, MessageId> getMessageId2MessageIdSelfExpiringCache(String name) {
        return new MessageId2MessageIdSelfExpiringCache(name);
    }

    @Override
    public Sequence2MessageSelfExpiringCache getSequence2MessageSelfExpiringCache(String name) {
        return new Sequence2MessageSelfExpiringCache(name);
    }

}
