package net.redvoiss.sms.upload.beans;

import org.junit.Test;

public class ReportBeanTest {
	
	@Test
	public void toStringTest()  {
        System.out.println(fill(new ReportBeanImpl()).toString());
    }
    
    protected ReportBean fill(ReportBean aReportBean) {
        aReportBean.addMessage("net.redvoiss.sms.upload.store.missing_campaign.error");
        aReportBean.addMessage("net.redvoiss.sms.upload.store.db_error", "cc");
        aReportBean.addWarning("net.redvoiss.sms.upload.store.missing_campaign.error");
        aReportBean.addWarning("net.redvoiss.sms.upload.store.missing_campaign.error");
        aReportBean.addError("net.redvoiss.sms.upload.store.missing_campaign.error");
        aReportBean.addError("net.redvoiss.sms.upload.store.db_error", "hh");
        return aReportBean;
    }
}