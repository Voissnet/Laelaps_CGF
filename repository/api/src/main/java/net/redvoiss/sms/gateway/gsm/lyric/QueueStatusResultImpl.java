package net.redvoiss.sms.gateway.gsm.lyric;

import org.json.JSONException;

import net.redvoiss.sms.SMSException;

public class QueueStatusResultImpl extends ResultImpl implements QueueStatusResult {
    public QueueStatusResultImpl(String json) throws SMSException {
        super( json );
    }

    public int getNumberOfMessages() throws SMSException {
        return getValue("n_msgs");
    }
    
    public int getNumberOfNewMessages() throws SMSException {
        return getValue("n_new");
    }
    
    public int getNumberOfProcessingMessages() throws SMSException {
        return getValue("n_proc");
    }
    
    public int getNumberOfSentMessages() throws SMSException {
        return getValue("n_sent");
    }
    
    public int getNumberOfFailedMessages() throws SMSException {
        return getValue("n_fail");
    }

    protected int getValue(String k) throws SMSException {
        try {
            return m_jsonObject.getInt(k);
        } catch ( JSONException e ) {
            throw new SMSException( e );
        }
    }
    
    @Override public String toString() {
        return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this);
    }
    
    @Override public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) {
            return false;
        }
        return org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals(this, obj);
    }
    
    @Override public int hashCode() {
        return org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode(this);
    }
}