package net.redvoiss.sms.bean;

public class Message {

    private final String m_smsCode;
    private final String m_smscGWId;
    private final int m_route;

    public Message(String smsCode, String smscGWId, int route) {
        m_smsCode = smsCode;
        m_smscGWId = smscGWId;
        m_route = route;
    }

    public Message(Message message) {
        m_smsCode = message.getSmsCode();
        m_smscGWId = message.getSmscGWId();
        m_route = message.getRoute();
    }

    public String getSmsCode() {
        return m_smsCode;
    }

    public String getSmscGWId() {
        return m_smscGWId;
    }

    public int getRoute() {
        return m_route;
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
