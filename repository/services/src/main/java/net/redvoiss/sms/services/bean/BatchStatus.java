package net.redvoiss.sms.services.bean;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class BatchStatus {
    int msgCod;
    String destination;
    String message;
    String idcliente;
    String fechanotifica;
    
    public BatchStatus(int msgCod, String destination, String message, String idcliente, String fechanot) {
        this.msgCod = msgCod;
        this.destination = destination;
        this.message = message;
        this.idcliente = idcliente;
        this.fechanotifica = fechanot;
    }

    public int getMsgCod() {
        return msgCod;
    }

    public String getDestination() {
        return destination;
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