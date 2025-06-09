package net.redvoiss.sms.gateway.gsm.lyric;

import net.redvoiss.sms.SMSException;

import java.util.ArrayList;
import java.util.List;

import org.json.*;

public class ReceiveMessageResultImpl extends ResultImpl implements ReceiveMessageResult {
    public ReceiveMessageResultImpl(String json) throws SMSException {
        super( json );
    }
    
    public List<ReceiveMessageRecord> getReceiveMessageRecord() throws SMSException {
        List<ReceiveMessageRecord> ret = new ArrayList<ReceiveMessageRecord>(); 
        try {
            JSONArray aJSONArray = m_jsonObject.getJSONArray("reg_array");
            for( int i = 0; i < aJSONArray.length(); i++ ) {
                JSONObject aJSONObject = aJSONArray.getJSONObject(i);
                //System.out.println( aJSONObject );
                int id = aJSONObject.getInt("id");
                String numOrigen = aJSONObject.getString("numorig");
                String recvDate = aJSONObject.getString("recv_date");
                String smsMessage = aJSONObject.getString("message");
                int channelRcv = aJSONObject.getInt("channel");
                String imsi = aJSONObject.getString("imsi");
                ReceiveMessageRecord aReceiveMessageRecord = new ReceiveMessageRecord( id, numOrigen, recvDate, smsMessage, channelRcv, imsi);
                ret.add( aReceiveMessageRecord ); 
            }
        } catch ( JSONException e ) {
            throw new SMSException( e );
        }
        return ret;
    }
    
    public int getNumberRead() throws SMSException {
        try {
            return m_jsonObject.getInt("n_read");
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
