package net.redvoiss.sms.gateway.gsm.lyric;

import net.redvoiss.sms.SMSException;

import java.util.Date;

public interface VersionResult extends Result {
    String getApiVersion() throws SMSException;
}