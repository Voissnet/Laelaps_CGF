package net.redvoiss.sms.bean;

public class EntryBean {
    private MessageType m_type;
    private String m_key;
    private Object [] m_arguments;
    
    public EntryBean( MessageType type, String key, Object... arguments ) {
        m_type = type;
        m_arguments = arguments;
        m_key = key;
    }
    
    public Object[] getArguments() {
        return m_arguments;
    }
    
    public String getKey() {
        return m_key;
    }
    
    public MessageType getType() {
        return m_type;
    }
    
    public boolean isError() {
        return MessageType.ERROR.equals( m_type );
    }
    
    public boolean isWarning() {
        return MessageType.WARNING.equals( m_type );
    }
    
    public static enum MessageType {
        DEFAULT, WARNING, ERROR 
    }
}