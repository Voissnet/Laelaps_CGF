package net.redvoiss.sms.smpp.client;

import java.io.UnsupportedEncodingException;
import org.smpp.Data;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.WrongLengthOfStringException;

public class EntelSmppClient extends SMPPClient {
	private static final int CODE_RUTA = Integer.parseInt(System.getProperty("route", "1"));//defaults to Movistar's route

	public EntelSmppClient(int codeRuta, String idoidd) throws Exception {
		super(codeRuta, idoidd);
	}

	public static void main(String args[]) throws Exception {
		new EntelSmppClient(CODE_RUTA, "310220").call();
	}

	protected void set(SubmitSM request, String shortMessage)
			throws WrongLengthOfStringException, UnsupportedEncodingException {
		request.setDataCoding(Data.DFLT_DATA_CODING);
		request.setShortMessage(shortMessage, Data.ENC_CP1252);
	}

}