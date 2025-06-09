package net.redvoiss.sms.upload.beans;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.builder.ToStringBuilder;

import static net.redvoiss.sms.upload.beans.util.MailToStringStyle.STYLE;

public class AsyncReportBeanImpl extends AbstractReportBean implements ReportBean {
    private Queue<MessageBean> m_messages = new LinkedBlockingQueue<MessageBean>();
        
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