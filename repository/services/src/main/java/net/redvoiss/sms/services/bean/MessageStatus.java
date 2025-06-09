package net.redvoiss.sms.services.bean;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class MessageStatus {
    int msgCod;
    String status;
    String idcliente;
    String fechanotifica;
    
    public MessageStatus(int msgCod, String status, String idcliente, String fechanot) {
        this.msgCod = msgCod;
        this.status = status;
        this.idcliente = idcliente;
        this.fechanotifica = fechanot;
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

    public String getFechanotifica() {
        return idcliente;
    }

    @Override
    public String toString() {
        return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this);
    }
}