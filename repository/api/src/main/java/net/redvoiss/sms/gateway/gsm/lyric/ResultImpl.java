package net.redvoiss.sms.gateway.gsm.lyric;

import org.json.*;

import net.redvoiss.sms.SMSException;

import java.util.Date;

public class ResultImpl implements Result {
    JSONObject m_jsonObject;
    
    public ResultImpl(String json) throws SMSException {
        try { 
            m_jsonObject = new JSONObject(json);
        } catch ( JSONException e ) {
            throw new SMSException( e );
        }
    }
    
    public boolean isSuccess() throws SMSException {
        if( m_jsonObject == null ) {
            return false;
        }
        try {
            return m_jsonObject.getBoolean("success");
        } catch ( JSONException e ) {
            throw new SMSException( e );
        }
    }
    
    public ErrorCode getErrorCode() throws SMSException {
        try {
            final String ec = m_jsonObject.getString("error_code");
            return ErrorCode.forError(ec);
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