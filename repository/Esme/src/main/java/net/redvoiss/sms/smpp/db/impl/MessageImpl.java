package net.redvoiss.sms.smpp.db.impl;

import net.redvoiss.sms.bean.NewMessage;
import net.redvoiss.sms.smpp.bean.Message;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 *
 * @author Jorge Avila
 */
public class MessageImpl implements Message {

    NewMessage newMessage;

    /**
     * Constructor
     *
     * @param newMessage
     */
    public MessageImpl(NewMessage newMessage) {
        this.newMessage = newMessage;
    }

    @Override
    public MessageId getId() {
        return new MessageId(newMessage.getSmsCode(), newMessage.getSmscGWId());
    }

    @Override
    public String getSourceAddress() {
        return newMessage.getSource().getTarget();
    }

    @Override
    public String getDestinationAddress() {
        return newMessage.getDestination().getTarget();
    }

    @Override
    public String getContent() {
        return newMessage.getMessage();
    }

    @Override
    public String getEncoding() {
        return "ISO8859_1";
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
