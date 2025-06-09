package net.redvoiss.sms.bean;

import java.util.regex.Pattern;

public class Slot {
    private String m_realNumber, m_imsi;
    private int m_slotId;
    
    public Slot( int slotId, String realNumber, String imsi )  {
        m_realNumber = realNumber;
        m_slotId = slotId;
        m_imsi = imsi;
    }
    
    public int getSlotId() {
        return m_slotId;
    }

    public String getImsi() {
        return m_imsi;
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