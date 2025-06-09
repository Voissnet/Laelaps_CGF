package net.redvoiss.sms.gateway.gsm.teles;

import net.redvoiss.sms.SMSException;
import net.redvoiss.sms.bean.Destination;

import java.util.List;

public interface GAO {

    void sendMessage( int slot, Destination destination, String msg) throws SMSException;
    List<Receipt> receiveReceiptList() throws SMSException;
    
    static GAO build(String fromDomainName, String hostname, String userInternetAddress, char[] password) {
        return new GatewayImpl(fromDomainName, hostname, userInternetAddress, password);
    }
}