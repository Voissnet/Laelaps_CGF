package net.redvoiss.sms.gateway.gsm.lyric;

import java.sql.SQLException;

import org.junit.Ignore;
import org.junit.Test;

import net.redvoiss.sms.SMSException;
import net.redvoiss.sms.dao.DAO;

public class SwitchTest {
	
	@Ignore
	@Test
	public void test() throws java.sql.SQLException, SMSException {
		new Switch(net.redvoiss.sms.gateway.gsm.lyric.GAO.buildPlainNative("smslyrics.americatotal.cl", 3082), 17, 1, "name").run();
	}	

	@Ignore
	@Test
	public void testSIMVerification() throws SMSException, SQLException {
		Switch.verifyImsiConfiguracion(DAO.getDAO(), GAO.buildNative("lyric_api", "lyric_api", "smslyrics.americatotal.cl", 3085), DAO.getDAO().getRealNumberFromIMSI(18));
	}
}