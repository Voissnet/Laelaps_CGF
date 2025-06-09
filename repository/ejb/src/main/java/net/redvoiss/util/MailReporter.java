package net.redvoiss.util;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.Message.RecipientType;
import javax.mail.Transport;
import javax.mail.MessagingException;
import javax.mail.Session;

import java.util.Date;

import java.util.logging.Logger;

import net.redvoiss.sms.util.ApplicationProperty;

import static java.util.logging.Level.SEVERE;

@javax.ejb.Singleton

public class MailReporter {

    private static final Logger LOGGER = Logger.getLogger(MailReporter.class.getName());

    @Resource(lookup = "concurrent/smsWM")
    ManagedExecutorService m_executor;

    @Resource(lookup = "mail/RedvoissMailSession")
    private Session m_mailSession;

    @Inject
    @ApplicationProperty(name = "email.noc")
    private String m_emailNOC;

    @Inject
    @ApplicationProperty(name = "email.support")
    private String m_emailSupport;

    @Inject
    @ApplicationProperty(name = "email.bcc")
    private String m_emailBCC;

    public void reportCampaignUpload(String batchId, String text) {
        if (m_emailNOC == null) {
            LOGGER.warning("Email address is missing. Cowardly refusing to send email report.");
        } else {
            try {
                InternetAddress[] from = {new InternetAddress("SMS Batch Loader <sms@lanube.cl>", true)};
                InternetAddress[] to = {new InternetAddress(m_emailNOC, true)};
                InternetAddress[] bcc = new InternetAddress[]{new InternetAddress(m_emailBCC, true)};
                MimeMessage message = new MimeMessage(m_mailSession);
                message.addFrom(from);
                message.setRecipients(RecipientType.TO, to);
                message.setRecipients(RecipientType.BCC, bcc);
                message.setSubject("Reporte del Procesamiento De Lote: " + batchId);
                message.setSentDate(new Date());
                message.setText(text);
                m_executor.submit(() -> {
                    try {
                        Transport.send(message);
                    } catch (MessagingException me) {
                        LOGGER.log(SEVERE, "Unexpected exception while sending message: " + message, me);
                    }
                });
            } catch (MessagingException me) {
                LOGGER.log(SEVERE, "Unexpected exception while processing " + batchId, me);
            }
        }
    }
}
