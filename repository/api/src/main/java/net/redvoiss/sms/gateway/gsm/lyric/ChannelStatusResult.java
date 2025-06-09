package net.redvoiss.sms.gateway.gsm.lyric;

import net.redvoiss.sms.SMSException;

import java.util.List;

public interface ChannelStatusResult extends Result {
    List<ChannelStatusRecord> getChannels() throws SMSException;

    class ChannelStatusRecord {
        private int m_id;
        private boolean m_sendEnabled;
        private String m_state;
        private String m_imsi;
        private int m_nSentSMS;

        ChannelStatusRecord( int id, boolean isSendEnabled, String state, String imsi, int nSentSMS) {
            m_id = id;
            m_sendEnabled = isSendEnabled;
            m_state = state;
            m_imsi = imsi;
            m_nSentSMS = nSentSMS;
        }

        public int getId() {
            return m_id;
        }
        
        public StateCode getState() {
            return StateCode.forError(m_state);
        }

        enum StateCode {
            SendingSms("sending_sms"), RetryingSms("retrying_sms"), Registered("registered"), NoSimCard("no_simcard"), WaitingRegister("waiting_register"), UnknownErrorCode("Unexpected");

            String description;

            StateCode( String description ) {
                this.description = description;
            }

            public static StateCode forError(String s) {
                for (StateCode sc :StateCode.values()) {
                    if (sc.description.equals(s) ) {
                        return sc;
                    }
                }
                System.err.println( String.format("Unknown status code was: {%s}", s) ); //TODO get rid of this
                return UnknownErrorCode;
            }
        }

        public String getImsi() {
            return m_imsi;
        }
        
        public boolean isSendEnabled() {
            return m_sendEnabled;
        }

        public int getNSentSMS() {
            return m_nSentSMS;
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
}