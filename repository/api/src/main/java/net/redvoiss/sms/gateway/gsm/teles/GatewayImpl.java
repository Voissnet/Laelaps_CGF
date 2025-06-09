package net.redvoiss.sms.gateway.gsm.teles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

import static javax.mail.Flags.Flag;

import net.redvoiss.sms.SMSException;
import net.redvoiss.sms.bean.Destination;

public class GatewayImpl implements GAO {
    protected static final Logger LOGGER = Logger.getLogger(GatewayImpl.class.getName());
    private static final Pattern CONFIRMATION_PATTERN = Pattern.compile( String.format("^(?:%s|%s|%s|%s|%s)$", ReceiptTypeEnum.SENT.getRegex(), ReceiptTypeEnum.FAILED.getRegex(), ReceiptTypeEnum.UNCONFIRMED.getRegex(), ReceiptTypeEnum.REPLACED.getRegex(), ReceiptTypeEnum.CONFIRMED.getRegex()) );
    private static final Pattern SENDER_PATTERN = Pattern.compile( "^(\\d{9})@.+$" );
    private String m_gatewayHostname;
    private String m_host, m_username, m_password;
    private final static boolean DEBUG = false;
    private static final int MODE = Folder.READ_WRITE;

    public GatewayImpl(String gatewayHostname, String host, String username, char[] password) {
        m_host = host;
        m_password = new String( password );
        m_gatewayHostname = gatewayHostname;
        m_username = username;
    }

    public void sendMessage( int slot, Destination destination, String msg) throws SMSException {
        String r = String.format("SMS0%s00%s@%s", String.valueOf(slot), destination.getTarget(), m_gatewayHostname);
        LOGGER.log( FINE, "", new Object[]{r, msg});
        try {
            InternetAddress from = new InternetAddress(m_username);
            InternetAddress recipient = new InternetAddress(r);
            sendSMS( from, recipient, msg);
        } catch (javax.mail.internet.AddressException e) {
            throw new SMSException( e);
        }
    }

    void sendSMS( InternetAddress from, InternetAddress recipient, String message) throws SMSException {
        Properties props = new Properties();
        props.put("mail.smtp.host", m_gatewayHostname);
        Session session = Session.getInstance(props, null);
        session.setDebug(DEBUG);
        try {            
            Message msg = new javax.mail.internet.MimeMessage(session);
            msg.setFrom(from);
            msg.addRecipient(Message.RecipientType.TO, recipient);
            msg.setSubject("");
            msg.setContent(message, "text/plain; charset=UTF-8");
            javax.mail.Transport.send(msg);
        } catch (MessagingException e) {
            throw new SMSException( e);
        }
    }

    public List<Receipt> receiveReceiptList() throws SMSException {
        List<Receipt> ret = new ArrayList<Receipt>();
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "pop3");
        props.setProperty("mail.pop3.starttls.enable", "true");
        props.put("mail.pop3.socketFactory.port", 995);
        props.setProperty("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.setProperty("mail.pop3.socketFactory.fallback", "false");
        Session session = Session.getInstance(props, null);
        session.setDebug(DEBUG);
        Store store = null;
        try {
            store = session.getStore();
            store.connect(m_host, m_username, m_password);
            Folder inbox = null;
            try {
                inbox = store.getFolder("INBOX");
                inbox.open(MODE);
                final int messageCount = inbox.getMessageCount();
                LOGGER.fine( String.format( "Number of received messages was {%d}", messageCount) );
                for( int i = 0; i < messageCount; i++) {
                    Receipt receipt = getReceipt( inbox.getMessage(i+1) );
                    if ( receipt == null ) {
                        LOGGER.warning( "Unexpected empty receipt");
                    } else {
                        ret.add( receipt );
                    }
                }
            } finally {
                if ( inbox == null ) {
                    LOGGER.warning( "Inbox could not be initialized" );
                } else {
                    inbox.close(true);
                }
            }
        } catch (Exception e) {
            throw new SMSException( e );
        } finally {
            if( store == null ) {
                LOGGER.warning( "Store could not be initialized" );
            } else {
                try {
                    store.close();
                } catch ( MessagingException me ) {
                    throw new SMSException( me );
                }
            }
        }
        Collections.sort( ret, new Comparator<Receipt>() {
            @Override
            public int compare(Receipt r1, Receipt r2) {
                int ret;
                ReceiptTypeEnum rte1 = r1.getReceiptType();
                ReceiptTypeEnum rte2 = r2.getReceiptType();
                if ( rte1 == null && rte2 == null ) {
                    LOGGER.severe("Unexpected empty type");
                    ret = 0;
                } else {
                    if ( rte1 == null ) {
                        LOGGER.severe("Unexpected empty type");
                        ret = -1;
                    } else if ( rte2 == null ) {
                        LOGGER.severe("Unexpected empty type");
                        ret = 1;
                    } else {
                        if( rte1.equals( ReceiptTypeEnum.SENT ) && rte2.equals( ReceiptTypeEnum.SENT ) ) {
                            ret = 0;
                        } else if ( rte1.equals (ReceiptTypeEnum.SENT) ) {
                            ret = -1;
                        } else {
                            ret = 1;
                        }
                    }
                }
                return ret;
            }
        } ); //This should be processed in order, hence enforced SENT < CONFIRMED = FAILED = REPLACED = UNCONFIRMED

        LOGGER.fine( "Sorted receipt list is: " + ret.toString() );
        return ret;
    }

    protected Receipt getReceipt( Message msg ) throws MessagingException {
        Receipt ret = null;
        final String subject = msg.getSubject();
        if( CONFIRMATION_PATTERN.matcher(subject).matches() ) {
            Address[] addressArr = msg.getFrom();
            if( addressArr.length == 1 ) {
                Matcher senderMatcher = SENDER_PATTERN.matcher( addressArr[0].toString() );
                if ( senderMatcher.matches() ) {
                    final String from = senderMatcher.group(1);
                    msg.setFlag(Flag.DELETED, MODE == Folder.READ_WRITE);
                    cycle: for( ReceiptTypeEnum type: ReceiptTypeEnum.values() ) {
                        Pattern p = Pattern.compile( type.getRegex() );
                        Matcher m = p.matcher( subject );
                        if ( m.matches() ) {
                            switch( type ) {
                                case SENT:
                                case CONFIRMED:
                                case UNCONFIRMED:
                                    ret = new Receipt( from, m.group(1), msg.getSentDate(), type);
                                    break;
                                case FAILED:
                                    ret = new Receipt( from, m.group(2), msg.getSentDate(), type);
                                    break;
                                case REPLACED:                                    
                                default:
                                    break;
                            }
                            break cycle;
                        } 
                    }
                } else {
                    LOGGER.warning( String.format("Unexpected sender format {%s}", addressArr[0].toString()));
                }
            } else {
                LOGGER.warning( String.format("Message has more than one destination {%s}", String.valueOf(msg) ) );
            }
        } else {
            LOGGER.fine( String.format("Subject {%s} does not seems to be a confirmation", subject) );
        }
        return ret;
    }
    
}