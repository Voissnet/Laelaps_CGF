package net.redvoiss.sms.gateway.gsm.lyric;

import net.redvoiss.sms.SMSException;

import java.util.List;

public interface ReceiveMessageResult extends Result {
    List<ReceiveMessageRecord> getReceiveMessageRecord() throws SMSException;
    int getNumberRead() throws SMSException;
}