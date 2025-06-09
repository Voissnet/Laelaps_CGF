package net.redvoiss.sms.gateway.gsm.lyric;

import net.redvoiss.sms.SMSException;

import java.util.ArrayList;
import java.util.List;

import org.json.*;

public class ChannelStatusResultImpl extends ResultImpl implements ChannelStatusResult  {

    public ChannelStatusResultImpl(String json) throws SMSException {
        super( json );
    }

    public List<ChannelStatusRecord> getChannels() throws SMSException {
        List<ChannelStatusRecord> ret = new ArrayList<ChannelStatusRecord>();
        try {
            JSONArray aJSONArray = m_jsonObject.getJSONArray("channels");
            for( int i = 0; i < aJSONArray.length(); i++ ) {
                JSONObject jo = aJSONArray.getJSONObject(i);
                int id = jo.getInt("id"); 
                boolean isSendEnabled = jo.getInt("sms_send_ena") == 1;
                String state = jo.getString("state");
                final String imsi_key = "imsi";
                String imsi = jo.has(imsi_key) ? jo.getString(imsi_key) : null;
                final String nSentSMS_key = "n_sent_sms";  
                int nSentSMS= jo.has(nSentSMS_key) ? jo.getInt(nSentSMS_key) : 0;
                ChannelStatusRecord csr = new ChannelStatusRecord( id, isSendEnabled, state, imsi, nSentSMS );
                ret.add( csr ); 
            }
        } catch ( JSONException e ) {
            throw new SMSException( e );
        } 
        return ret;
    }
}