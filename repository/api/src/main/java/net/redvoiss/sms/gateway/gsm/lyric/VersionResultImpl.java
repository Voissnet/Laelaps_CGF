package net.redvoiss.sms.gateway.gsm.lyric;

import org.json.JSONException;

import net.redvoiss.sms.SMSException;

public class VersionResultImpl extends ResultImpl implements VersionResult {
    public VersionResultImpl(String json) throws SMSException {
        super( json );
    }
    
    public String getApiVersion() throws SMSException {
        try {
            return m_jsonObject.getString("api_version");
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