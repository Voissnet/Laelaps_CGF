package net.redvoiss.sms.services.bean;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import java.text.SimpleDateFormat;
import java.text.ParseException;



/**
 * @see <a href="http://stackoverflow.com/questions/4208827/how-does-jax-ws-map-an-xml-date-with-time-zone-to-a-java-date">How does JAX-WS map an XML date with time zone to a Java Date?</a>
 * @see <a href="http://dcx.sybase.com/1101/en/dbprogramming_en11/jax-datatypes-tutorial.html">Tutorial: Using data types with JAX-WS</a>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BulkMessage")
public class BulkMessage {
        // 2020-12-07. Corijo error de traslape de 3 horas proocado por MILLISECOND_DATE_FORMAT = yyyy-MM-dd'T'HH:mm:ss.SSSXXX
	//public static SimpleDateFormat MILLISECOND_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");    
	//public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        public static SimpleDateFormat MILLISECOND_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");  
	public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        // 2020.12-07.Fin correccion de error por el formato yyyy-MM-dd'T'HH:mm:ss.SSSXXX

	@XmlElement(name = "message_details", required=true, nillable=false)
	private Message[] m_messages;
	
	/**
	 * Date in "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" format. For example "2010-08-20T01:00:00-04:00".
	 * @see {@link SimpleDateFormat}
	 */
	@XmlElement(name = "sendDate")
	private Date m_sendDate;/* 2010-08-20T01:00:00-04:00 */
	
	@XmlElement(name = "bulkName", required=true, nillable=false)
	private String m_bulkName;
	
	@XmlElement(name = "isCommercial", defaultValue="false")
	private boolean m_commercial = false;
    
    @XmlElement(name = "message", required=true, nillable=false)
	private String m_message;
	
	public void setSendDate(String date) throws ParseException {
		try {
			m_sendDate = MILLISECOND_DATE_FORMAT.parse(date);
                        
		} catch ( java.text.ParseException e ) {
			m_sendDate = DATE_FORMAT.parse(date);
		}
	}
    
    public Message[] getMessageDetails() {
        return m_messages;
    }
    
    public boolean isCommercial() {
        return m_commercial;
    }
    
    public String getMessage() {
        return m_message;
    }
    
    public String getBulkName() {
        return m_bulkName;
    }
    
    public Date getSendDate() {
        return m_sendDate;
    }
	
	public String toString() {
		String ret = null;
		try {
			ret = new org.apache.commons.lang3.builder.ToStringBuilder(this).
			append(m_messages).
			append(MILLISECOND_DATE_FORMAT.format(m_sendDate)).
			append(m_bulkName).
			append(m_commercial).
			append(m_message).
			toString();
		} catch (Exception e) {
			ret = new org.apache.commons.lang3.builder.ToStringBuilder(this).
			append(m_messages).
			append(m_sendDate).
			append(m_bulkName).
			append(m_commercial).
			append(m_message).
			toString();
		}
		return ret;
	}
}