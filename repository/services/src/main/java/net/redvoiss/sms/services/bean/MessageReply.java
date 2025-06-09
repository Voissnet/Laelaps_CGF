package net.redvoiss.sms.services.bean;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.Date;

@XmlAccessorType(XmlAccessType.FIELD)
public class MessageReply {
    int replyId;
    Date date;
    String message;
    String idcliente;
    
    public MessageReply(int replyId, Date date, String message, String idcliente) {
        this.replyId = replyId;
        this.date = date;
        this.message = message;
        this.idcliente = idcliente;
    }

    public int getReplyId() {
        return replyId;
    }

    public Date getDate() {
        return date;
    }

    public String getMessage() {
        return message;
    }

    public String getIdcliente() {
        return idcliente;
    }
    
    @Override
    public String toString() {
        return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this);
    }
}