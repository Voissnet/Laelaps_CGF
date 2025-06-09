package net.redvoiss.sms.services.web;

import org.junit.Test;
import org.junit.Ignore;

import net.redvoiss.sms.services.bean.BulkMessage;
import net.redvoiss.sms.services.error.SMSError;

public class SMSTest {
	
    @Test
    @Ignore
	public void sendBulkMessage() throws java.text.ParseException, java.sql.SQLException, SMSError {
		SMS sms = new SMS();
		{
			BulkMessage bulkMessage = null;
			sms.sendBulkMessage(bulkMessage);
		}
		{
			BulkMessage bulkMessage = new BulkMessage();
			bulkMessage.setSendDate("2010-08-20T01:00:00.1-04:00");
			sms.sendBulkMessage( bulkMessage );
		}
		{
			BulkMessage bulkMessage = new BulkMessage();
			bulkMessage.setSendDate("2010-08-20T01:00:00-04:00");
			sms.sendBulkMessage( bulkMessage );
		}
	}
        
    @Test
    @Ignore
    public void sendBulkMessage_v2() throws java.text.ParseException, java.sql.SQLException, SMSError {
		SMS sms = new SMS();
		{
			BulkMessage bulkMessage = null;
			sms.sendBulkMessage_v2(bulkMessage);
		}
		{
			BulkMessage bulkMessage = new BulkMessage();
			bulkMessage.setSendDate("2010-08-20T01:00:00.1-04:00");
			sms.sendBulkMessage_v2( bulkMessage );
		}
		{
			BulkMessage bulkMessage = new BulkMessage();
			bulkMessage.setSendDate("2010-08-20T01:00:00-04:00");
			sms.sendBulkMessage_v2( bulkMessage );
		}
	}

}