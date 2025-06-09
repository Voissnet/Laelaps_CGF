package net.redvoiss.sms.services.bean;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Message")
public class Message {
	@XmlElement(name = "destination", required=true, nillable=false)
	private String m_destination;
	@XmlElement(name = "field")
	private String m_field;
        
	@XmlElement(name = "idCliente", defaultValue="")
	private String m_idcliente = "";
        
    public String getDestination() {
        return m_destination;
    }
	
    public String getField() {
        return m_field;
    }
    
    public String idCliente() {
        return m_idcliente;
    }
        
        public String toString() {
		return new org.apache.commons.lang3.builder.ToStringBuilder(this).
			append(m_destination).
			append(m_field).
                        append(m_idcliente).
			toString();
	}
}