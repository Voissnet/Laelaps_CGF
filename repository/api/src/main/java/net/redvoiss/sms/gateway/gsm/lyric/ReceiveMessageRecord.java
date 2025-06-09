package net.redvoiss.sms.gateway.gsm.lyric;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.redvoiss.sms.bean.Destination;

public class ReceiveMessageRecord {
    private final static SimpleDateFormat SDF = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
    private int m_id;
    private Destination m_numOrig;
    private String m_receivedDate;
    private String m_message;
    private int m_channel;
    private String m_imsi;
    
    public ReceiveMessageRecord( int id, String numOrig, String receivedDate, String message, int channel, String imsi) {
        m_id = id;
        m_numOrig = new Destination(numOrig);
        m_receivedDate = receivedDate;
        m_message = message;
        m_channel = channel;
        m_imsi = imsi;
    }
    
    public int getId() {
        return m_id;
    }

    public Destination getNumOrig() {
        return m_numOrig;
    }

    public Date getReceivedDate() throws ParseException {
        return SDF.parse(m_receivedDate);
    }

    public String getMessage() {
        return m_message;
    }

    public int getChannel() {
        return m_channel;
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
        return new org.apache.commons.lang3.builder.EqualsBuilder().append(m_id, ((ReceiveMessageRecord) obj).m_id ).isEquals();
    }
    
    @Override public int hashCode() {
        return new org.apache.commons.lang3.builder.HashCodeBuilder().append(m_id).toHashCode();
    }
}