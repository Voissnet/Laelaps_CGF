package net.redvoiss.sms.gateway.gsm.lyric;

import net.redvoiss.sms.SMSException;

import java.util.Date;

public interface SendMessageResult extends Result {
    int getId() throws SMSException;
}