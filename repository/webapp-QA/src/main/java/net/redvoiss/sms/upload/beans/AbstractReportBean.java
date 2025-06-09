package net.redvoiss.sms.upload.beans;

import java.util.Queue;

public abstract class AbstractReportBean {
    protected boolean m_hasError = false;
        
    public boolean isErroneus() {
        return m_hasError;
    }
 
    abstract Queue<MessageBean> getMessages();
    
    public void addMessage(String message) {
        getMessages().add( new MessageBean(MessageBean.MessageType.DEFAULT, message) );
    }
    
    public void addMessage(String message, Object ... details) {
        getMessages().add( new MessageBean(MessageBean.MessageType.DEFAULT, message, details ) );
    }
    
    public void addWarning(String warningMessage) {
        getMessages().add( new MessageBean(MessageBean.MessageType.WARNING, warningMessage) );
    }
    
    public void addWarning(String warningMessage, Object ... details) {
        getMessages().add( new MessageBean(MessageBean.MessageType.WARNING, warningMessage, details) );
    }
    
    public void addError(String errorMessage) {
        getMessages().add( new MessageBean(MessageBean.MessageType.ERROR, errorMessage) );
        m_hasError = true;
    }
    
    public void addError(String errorMessage, Object ... details) {
        getMessages().add( new MessageBean(MessageBean.MessageType.ERROR, errorMessage, details) );
        m_hasError = true;
    }   
}