package net.redvoiss.sms.gateway;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import java.io.IOException;

import java.util.logging.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Properties;

public class Teles implements Runnable {
	public static final Pattern SUBJECT_PATTERN = Pattern.compile("SMS\\s(\\d+)\\s0+:*(\\d{2,11})$");
	private static final String PROTOCOL = "pop3";
	private static final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
	
	private String m_username = "claroprepago01@lanube.cl";
	private String m_password = "tmprv123";
	private String m_host = "mail.lanube.cl";
	private int m_port = 995;
	private int m_mode = Folder.READ_ONLY;//Folder.READ_WRITE

	private Logger m_logger = null;
	
	protected Teles() {
		m_logger = Logger.getLogger(Teles.class.getName());
	}
	
	public Teles(String username, String password, String hostname, int port) {
		m_username = username;
		m_password = password;
		m_host = hostname;
		m_port = port;
		m_logger = Logger.getLogger(String.format("Teles {%s}", username));
	}
	
	public void run() {
		process( (Folder f) -> {
			final int size = f.getMessageCount();
			for (int j = 0; j < size; j++) {
				Message msg = f.getMessage(j+1);
				final String messageContent = String.valueOf(msg.getContent());
				final String subject = msg.getSubject();
				Matcher m = SUBJECT_PATTERN.matcher(subject);
				if( m.matches() && m.groupCount() == 2 ) {	
					final String imsi = m.group(1);
					final String remitente = m.group(2);
					msg.setFlag(Flags.Flag.DELETED, isItForReal());
					//TODO enqueue read message
				} else {
					m_logger.warning(String.format("Unrecognized subject format. Escaped subject: {%s}", 
						org.apache.commons.lang3.StringEscapeUtils.escapeJava(subject)));
				}
			}
			return size;
		} );
	}
	
	protected int process (Deliverer d) {
		Store store = null;
		Folder folder = null;
		int ret = -1;
		try {
			Properties props = new Properties();
			java.security.Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
			props.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY);
			props.setProperty("mail.pop3.socketFactory.fallback", "false");
			final String port = String.valueOf(m_port);
			props.setProperty("mail.pop3.port", port);
			props.setProperty("mail.pop3.socketFactory.port", port);
			props.setProperty("mail.mime.address.strict", "false");
			//props.setProperty("mail.debug", "true");
			Session session = Session.getInstance(props, null);
			URLName urln = new URLName(PROTOCOL, m_host, m_port, null, m_username, m_password);
			store = session.getStore(urln);
			store.connect();
			folder = store.getFolder("INBOX");
			folder.open(m_mode);
			ret = d.readAndEnqueueMessage(folder);
		} catch ( NoSuchProviderException e ) {
			e.printStackTrace();
		} catch ( MessagingException e ) {
			e.printStackTrace();
		} catch ( IOException e ) {
			e.printStackTrace();
		} finally {
			try { if (folder != null) { folder.close( isItForReal() ); } } catch ( MessagingException e ) { e.printStackTrace(); }
			try { if (store != null) { store.close(); } } catch ( MessagingException e ) { e.printStackTrace(); }
		}
		return ret;
	}
	
	private boolean isItForReal() {
		return m_mode == Folder.READ_WRITE;
	}
	
	protected int healthCheck() {
		return process( (Folder f) -> { return f.getMessageCount(); } );
	}
	
	interface Deliverer {
        int readAndEnqueueMessage(Folder f) throws MessagingException, IOException;
    }

}