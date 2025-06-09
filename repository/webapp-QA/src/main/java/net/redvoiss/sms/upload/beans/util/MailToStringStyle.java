package net.redvoiss.sms.upload.beans.util;

import org.apache.commons.lang3.builder.StandardToStringStyle;

/**
 * @see http://dictionary.reference.com/browse/mailable
 */ 
 
public class MailToStringStyle {
    public static final StandardToStringStyle STYLE;    
    static {
        STYLE = new StandardToStringStyle();
        STYLE.setUseClassName(false);
        STYLE.setUseIdentityHashCode(false);
        STYLE.setFieldSeparator(STYLE.getFieldSeparator() + "\n");
        STYLE.setContentStart(STYLE.getContentStart() + "\n" );
        STYLE.setContentEnd("\n" + STYLE.getContentEnd());        
    }
} 