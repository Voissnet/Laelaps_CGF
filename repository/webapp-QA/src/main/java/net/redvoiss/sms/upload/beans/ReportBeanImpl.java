package net.redvoiss.sms.upload.beans;

import java.util.Queue;
import java.util.ArrayDeque;

import org.apache.commons.lang3.builder.ToStringBuilder;

import static net.redvoiss.sms.upload.beans.util.MailToStringStyle.STYLE;

public class ReportBeanImpl extends AbstractReportBean implements ReportBean {
    private Queue<MessageBean> m_messages = new ArrayDeque<MessageBean>();
        
    public Queue<MessageBean> getMessages() {
        return m_messages;
    }
        
    @Override
    public String toString() {
        return new ToStringBuilder(this, STYLE).
            append("\t\tErrors", m_hasError).
            append("\t\tMessages", m_messages).
            toString();
	}
}