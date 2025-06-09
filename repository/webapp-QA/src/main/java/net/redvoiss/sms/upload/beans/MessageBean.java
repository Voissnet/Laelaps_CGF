package net.redvoiss.sms.upload.beans;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.StandardToStringStyle;

import java.util.ResourceBundle;

import java.text.MessageFormat;

public class MessageBean extends net.redvoiss.sms.bean.EntryBean {
    ResourceBundle m_resourceBundle = ResourceBundle.getBundle("net.redvoiss.sms.upload.messages");
    
    public static final StandardToStringStyle STYLE;
    
    static {
        STYLE = new StandardToStringStyle();
        STYLE.setUseClassName(false);
        STYLE.setUseIdentityHashCode(false);
        STYLE.setContentStart("\n\t\t\t" + STYLE.getContentStart());        
    }
    
    public MessageBean( MessageType type, String description ) {
        super( type, description);
    }
    
    public MessageBean( MessageType type, String description, Object ... details ) {
        super( type, description, details);
    }

    public String getMessage() {
        final String pattern = m_resourceBundle.getString(getKey());
        MessageFormat formatter = new MessageFormat( pattern );
        String ret = formatter.format( getArguments() ); 
        return ret;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, STYLE).
            append("Type", getType()).
            append("Message", getMessage()).
            toString();
	}
}