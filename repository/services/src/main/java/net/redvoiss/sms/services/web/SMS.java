package net.redvoiss.sms.services.web;

import net.redvoiss.sms.services.SMSService;
import net.redvoiss.sms.services.bean.BatchReply;
import net.redvoiss.sms.services.bean.BatchStatus;
import net.redvoiss.sms.services.bean.BulkMessage;
import net.redvoiss.sms.services.bean.MessageReply;
import net.redvoiss.sms.services.bean.MessageStatus;
import net.redvoiss.sms.services.error.SMSError;

import java.util.Map;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Resource;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;

import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.security.Principal;
import java.sql.SQLException;

import javax.sql.DataSource;

import javax.transaction.Transactional;

@WebService
public class SMS implements SMSService {
    private static final Logger LOGGER = Logger.getLogger(SMS.class.getName());

    @Resource(lookup = "jdbc/DB")
    private DataSource m_dataSource;

    @Resource
    WebServiceContext wsctx;

    public SMS() {
    }

    @WebMethod
    public String echo(String name) {
        return String.format("Message is {%s}.", name);
    }

    @WebMethod
    @Transactional(rollbackOn = { SQLException.class })
    public String sendBulkMessage(@WebParam(name = "bulkMessage") BulkMessage bulkMessage)
            throws SQLException, SMSError {
        String ret = send(getAuthorizationElements().getUsername(), bulkMessage, m_dataSource, false);
        return ret;
    }

    @WebMethod
    @Transactional(rollbackOn = { SQLException.class })
    public String sendBulkMessageWithReply(@WebParam(name = "bulkMessageWithReply") BulkMessage bulkMessage)
            throws SQLException, SMSError {
        String ret = send(getAuthorizationElements().getUsername(), bulkMessage, m_dataSource, true);
        return ret;
    }

    @WebMethod
    @Transactional(rollbackOn = { SQLException.class })
    public String sendBulkMessage_v2(@WebParam(name = "bulkMessage") BulkMessage bulkMessage)
            throws SQLException, SMSError {
        String ret = send_v2(getAuthorizationElements().getUsername(), bulkMessage, m_dataSource, false);
        return ret;
    }

    @WebMethod
    @Transactional(rollbackOn = { SQLException.class })
    public String sendBulkMessageWithReply_v2(@WebParam(name = "bulkMessageWithReply") BulkMessage bulkMessage)
            throws SQLException, SMSError {
        String ret = send_v2(getAuthorizationElements().getUsername(), bulkMessage, m_dataSource, true);
        return ret;
    }

    @WebMethod
    public MessageStatus checkMessageState(@WebParam(name = "mesgId") String id)
            throws NumberFormatException, SMSError {
        MessageStatus ret = checkMessageStatus(Integer.parseInt(id), getAuthorizationElements().getUsername(),
                m_dataSource);
        LOGGER.fine(() -> String.format("Status for {%s} was {%s}", id, ret));
        return ret;
    }

    @WebMethod
    public List<BatchStatus> checkBulkMessageState(@WebParam(name = "batchId") final String batchId)
            throws NumberFormatException, SMSError {
        List<BatchStatus> ret = checkBatchStatus(Integer.parseInt(batchId), getAuthorizationElements().getUsername(),
                m_dataSource);
        LOGGER.fine(() -> String.format("Status for {%s} was {%s}", batchId, ret));
        return ret;
    }

    @WebMethod
    public String checkBalance() {
        throw new UnsupportedOperationException("Momentarily unsupported");
    }

    @WebMethod
    public List<MessageReply> checkMessageReply(@WebParam(name = "messageId") final String messageId)
            throws NumberFormatException, SMSError {
        List<MessageReply> ret = checkMessageReply(Integer.parseInt(messageId),
                getAuthorizationElements().getUsername(), m_dataSource);
        LOGGER.fine(() -> String.format("Reply for {%s} was {%s}", messageId, ret));
        return ret;
    }

    @WebMethod
    public BatchReply checkBatchReplies(@WebParam(name = "batchId") final String batchId)
            throws NumberFormatException, SMSError {
        BatchReply ret = checkBatchReplies(Integer.parseInt(batchId), getAuthorizationElements().getUsername(),
                m_dataSource);
        LOGGER.fine(() -> String.format("Replies for batch {%s} was {%s}", batchId, ret));
        return ret;
    }

    private AuthorizationElements getAuthorizationElements() {
        LOGGER.fine("Getting authorization elements");
        Principal aPrincipal = wsctx.getUserPrincipal();
        final String principalName = aPrincipal.getName();
        LOGGER.fine(String.format("Principal name is {%s}", principalName));
        MessageContext mctx = wsctx.getMessageContext();// https://www.youtube.com/watch?v=zz_McCV4zcs
        Map<?, ?> http_headers = (Map<?, ?>) mctx.get(MessageContext.HTTP_REQUEST_HEADERS); // http://www.mkyong.com/webservices/jax-ws/application-authentication-with-jax-ws/
        List<?> aList = (List<?>) http_headers.get("Authorization");
        Pattern p = Pattern.compile("Basic\\s(\\S+)");
        AuthorizationElements aAuthorizationElements = null;
        for (Object o : aList) {
            String v = String.valueOf(o);
            LOGGER.fine(String.format("Http Headers (Authorization): %s", v));
            Matcher m = p.matcher(v);
            if (m.matches()) {
                LOGGER.fine("About to decrypt password");
                String s = new String(javax.xml.bind.DatatypeConverter.parseBase64Binary(m.group(1)));
                Pattern pp = Pattern.compile("(\\w+):(\\S+)");
                Matcher mm = pp.matcher(s);
                if (mm.matches() && mm.groupCount() == 2) {
                    LOGGER.fine("About to get password");
                    final String user = mm.group(1);
                    final String password = mm.group(2);
                    LOGGER.fine(String.format("Using pattern {%s}{%s}", user, password));
                    aAuthorizationElements = new AuthorizationElements(user, password);
                    break;
                } else {
                    LOGGER.warning("Unable to unscramble credentials");
                }
            } else {
                LOGGER.warning("Missing credentials");
            }
        }
        LOGGER.fine("About to return authorization elements");
        return aAuthorizationElements;
    }

    class AuthorizationElements {
        private String m_user;
        private String m_password;

        AuthorizationElements(String user, String password) {
            m_user = user;
            m_password = password;
        }

        public String getUsername() {
            return m_user;
        }

        public String getPassword() {
            return m_password;
        }
    }
}