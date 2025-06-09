package net.redvoiss.sms.smpp.client;

import java.io.UnsupportedEncodingException;
import org.smpp.Data;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.WrongLengthOfStringException;

public class HGSmppClient extends SMPPClient {
	private static final int CODE_RUTA = Integer.parseInt(System.getProperty("route", "28"));//defaults to HG's route

	public HGSmppClient(int codeRuta, String idoidd) throws Exception {
		super(codeRuta, idoidd);
	}

	public static void main(String args[]) throws Exception {
		new HGSmppClient(CODE_RUTA, null).call();
	}

	protected void set(SubmitSM request, String shortMessage)
			throws WrongLengthOfStringException, UnsupportedEncodingException {
		request.setDataCoding(Data.DFLT_DATA_CODING);
		request.setShortMessage(shortMessage, Data.ENC_CP1252);
	}
}