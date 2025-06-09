package net.redvoiss.sms.gateway.gsm.lyric;

import org.json.*;

import java.time.Instant;
import java.util.Date;

import net.redvoiss.sms.SMSException;

import static net.redvoiss.sms.gateway.gsm.lyric.GAO.DeliveryStatus;
import static net.redvoiss.sms.gateway.gsm.lyric.GAO.LastError;
import static net.redvoiss.sms.gateway.gsm.lyric.GAO.MessageStatus;
import static net.redvoiss.sms.gateway.gsm.lyric.GAO.ReportStage;

public class MessageStatusResultImpl extends ResultImpl implements MessageStatusResult {
    public MessageStatusResultImpl(String json) throws SMSException {
        super(json);
    }

    public ReportStage getReportStage() throws SMSException {
        try {
            int status = m_jsonObject.getInt("report_stage");
            return ReportStage.forStatus(status);
        } catch (JSONException e) {
            throw new SMSException(e);
        }
    }

    public MessageStatus getMessageStatus() throws SMSException {
        try {
            int messageStatus = m_jsonObject.getInt("message_status");
            return MessageStatus.forStatus(messageStatus);
        } catch (JSONException e) {
            throw new SMSException(e);
        }
    }

    public int getNumber() throws SMSException {
        try {
            return m_jsonObject.getInt("num");
        } catch (JSONException e) {
            throw new SMSException(e);
        }
    }

    public int getChannel() throws SMSException {
        try {
            return m_jsonObject.getInt("channel");
        } catch (JSONException e) {
            throw new SMSException(e);
        }
    }

    public int getAttempts() throws SMSException {
        try {
            return m_jsonObject.getInt("n_tries");
        } catch (JSONException e) {
            throw new SMSException(e);
        }
    }

    public LastError getLastError() throws SMSException {
        try {
            int lastError = m_jsonObject.getInt("last_error");
            return LastError.forError(lastError);
        } catch (JSONException e) {
            throw new SMSException(e);
        }
    }

    public DeliveryStatus getDeliveryStatus() throws SMSException {
        try {
            int status = m_jsonObject.getInt("delivery_status");
            return DeliveryStatus.forStatus(status);
        } catch (JSONException e) {
            throw new SMSException(e);
        }
    }

    private Date toDate(long epoch) {
        Date ret;
        if (epoch == 0) {
            ret = null;
        } else {
            Instant instant = Instant.ofEpochSecond(epoch);
            ret = new Date(instant.toEpochMilli());
        }
        return ret;
    }

    public Date getDeliveryDate() throws SMSException {
        try {
            long l = m_jsonObject.getLong("delivery_date");
            return toDate(l);
        } catch (JSONException e) {
            throw new SMSException(e);
        }
    }

    public Date getSendDate() throws SMSException {
        try {
            long l = m_jsonObject.getLong("send_date");
            return toDate(l);
        } catch (JSONException e) {
            throw new SMSException(e);
        }
    }

    public Date getRecvDate() throws SMSException {
        try {
            long l = m_jsonObject.getLong("recv_date");
            return toDate(l);
        } catch (JSONException e) {
            throw new SMSException(e);
        }
    }

    @Override
    public String toString() {
        return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        return org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode(this);
    }
}