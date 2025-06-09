package net.redvoiss.sms.bean;

import java.util.Date;

public class PendingMessage extends Message {
    private String m_gsmId, m_imsi;
    private Destination m_destination;
    private int m_retry = 0;
    private Date m_date;

    public PendingMessage( String smsCode, String smscGWId, String gsmId, String destination, String source, int route, String retry, Date date ) {
        super( smsCode, smscGWId, route);
        m_gsmId = gsmId;
        m_destination = new Destination(destination);
        m_imsi = source;
        m_retry = retry == null ? 0 : Integer.parseInt(retry);
        m_date = date;
    }
    
    public PendingMessage( String smsCode, String smscGWId, String gsmId, int route ) {
        super( smsCode, smscGWId, route);
        m_gsmId = gsmId;
    }

    public PendingMessage( Message m, int gsmId ) {
        super( m);
        m_gsmId = String.valueOf(gsmId);
    }

    public PendingMessage( Message m, String gsmId ) {
        super( m);
        m_gsmId = gsmId;
    }

    public PendingMessage( NewMessage m, String gsmId, String imsi ) {
        super( m);
        m_gsmId = gsmId;
        m_imsi = imsi;
        m_destination = m.getDestination();
    }

    public String getIMSI() {
        return m_imsi;
    }

    public String getGsmId() {
        return m_gsmId;
    }

    public Destination getDestination() {
        return m_destination;
    }

    public int getRetry() {
        return m_retry;
    }

    public Date getDate() {
        return m_date;
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