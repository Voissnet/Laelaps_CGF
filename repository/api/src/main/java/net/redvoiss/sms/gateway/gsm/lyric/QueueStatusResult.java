package net.redvoiss.sms.gateway.gsm.lyric;

import net.redvoiss.sms.SMSException;

public interface QueueStatusResult extends Result {
    int getNumberOfMessages() throws SMSException;
    int getNumberOfNewMessages() throws SMSException;
    int getNumberOfProcessingMessages() throws SMSException;
    int getNumberOfSentMessages() throws SMSException;
    int getNumberOfFailedMessages() throws SMSException;
}