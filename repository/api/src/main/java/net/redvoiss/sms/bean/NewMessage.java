package net.redvoiss.sms.bean;

public class NewMessage extends Message {

    private final Destination m_destination, m_source;
    private final String m_message;

    public NewMessage(String smsCode, String smscGWId, String source, String destination, String message, int route) {
        super(smsCode, smscGWId, route);
        m_destination = new Destination(destination);
        m_source = new Destination(source);
        m_message = message;
    }

    public Destination getSource() {
        return m_source;
    }

    public Destination getDestination() {
        return m_destination;
    }

    public String getMessage() {
        return m_message;
    }

    @Override
    public String toString() {
        return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        return org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode(this);
    }
}
