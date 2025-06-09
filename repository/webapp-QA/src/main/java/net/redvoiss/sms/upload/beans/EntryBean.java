package net.redvoiss.sms.upload.beans;

public class EntryBean {
    private String m_destination;
    private String m_message;
    
    public EntryBean (String destination, String message) {
        m_destination = destination;
        m_message = message;
    }
    
    public String getDestination() {
        return m_destination;
    }
    
    public String getMessage() {
        return m_message;
    }
}