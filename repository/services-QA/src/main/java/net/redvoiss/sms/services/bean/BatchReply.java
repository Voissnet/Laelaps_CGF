package net.redvoiss.sms.services.bean;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class BatchReply {
    int batchId;
    List<MessageReply> messageReplyList;

    public BatchReply(int batchId, List<MessageReply> messageReplyList) {
        this.batchId = batchId;
        this.messageReplyList = messageReplyList;
    }

    public int getBatchId() {
        return batchId;
    }

    public List<MessageReply> getMessageReplyList() {
        return messageReplyList;
    }

    @Override
    public String toString() {
        return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this);
    }
}