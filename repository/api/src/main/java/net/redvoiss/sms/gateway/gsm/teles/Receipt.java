package net.redvoiss.sms.gateway.gsm.teles;

import java.util.Date;

public class Receipt {
    private String m_sender, m_imsi;
    private Date m_date;
    private ReceiptTypeEnum m_type;

    public Receipt( String from, String imsi, Date date, ReceiptTypeEnum type) {
        m_date = date;
        m_sender = from;
        m_imsi = imsi;
        m_type = type;
    }
    
    public String getSender() {
        return m_sender;
    }

    public String getImsi() {
        return m_imsi;
    }

    public Date getDate() {
        return m_date;
    }

    public ReceiptTypeEnum getReceiptType() {
        return m_type;
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