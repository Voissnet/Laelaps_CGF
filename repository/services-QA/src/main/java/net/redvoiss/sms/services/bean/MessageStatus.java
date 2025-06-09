package net.redvoiss.sms.services.bean;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class MessageStatus {
    int msgCod;
    String status;
    String idcliente;
    
    public MessageStatus(int msgCod, String status, String idcliente) {
        this.msgCod = msgCod;
        this.status = status;
        this.idcliente = idcliente;
    }

    public int getMsgCod() {
        return msgCod;
    }

    public String getStatus() {
        return status;
    }

    public String getIdcliente() {
        return idcliente;
    }

    @Override
    public String toString() {
        return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this);
    }
}