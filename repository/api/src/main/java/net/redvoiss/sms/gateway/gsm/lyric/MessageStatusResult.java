package net.redvoiss.sms.gateway.gsm.lyric;

import net.redvoiss.sms.SMSException;

import static net.redvoiss.sms.gateway.gsm.lyric.GAO.DeliveryStatus;
import static net.redvoiss.sms.gateway.gsm.lyric.GAO.LastError;
import static net.redvoiss.sms.gateway.gsm.lyric.GAO.MessageStatus;
import static net.redvoiss.sms.gateway.gsm.lyric.GAO.ReportStage;

import java.util.Date;

public interface MessageStatusResult extends Result {
    ReportStage getReportStage() throws SMSException;
    MessageStatus getMessageStatus() throws SMSException;
    int getNumber() throws SMSException;
    int getChannel() throws SMSException;
    int getAttempts() throws SMSException;
    LastError getLastError() throws SMSException;
    DeliveryStatus getDeliveryStatus() throws SMSException;
    Date getDeliveryDate() throws SMSException;
    Date getSendDate() throws SMSException;
    Date getRecvDate() throws SMSException;
}