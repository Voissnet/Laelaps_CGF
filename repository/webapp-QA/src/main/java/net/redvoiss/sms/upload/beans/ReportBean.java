package net.redvoiss.sms.upload.beans;

import java.util.Queue;

public interface ReportBean {
        
    Queue<MessageBean> getMessages();
    
    void addMessage(String message);
    
    void addMessage(String message, Object ... details);
    
    void addWarning(String warningMessage);

    void addWarning(String warningMessage, Object ... details);
    
    void addError(String errorMessage);
    
    void addError(String errorMessage, Object ... details);
    
}