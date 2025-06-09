package net.redvoiss.sms.smpp.client;

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;
import org.smpp.Data;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.WrongLengthOfStringException;

public class GriegosClient extends SMPPClient {
	private static final int CODE_RUTA = Integer.parseInt(System.getProperty("route", "3"));//defaults to Greek's route

	public GriegosClient(int codeRuta, String idoidd) throws Exception {
		super(codeRuta, idoidd);
	}

	public static void main(String args[]) throws Exception {
		new GriegosClient(CODE_RUTA, null).call();
	}

	protected void set(SubmitSM request, String shortMessage)
			throws WrongLengthOfStringException, UnsupportedEncodingException {
		request.setDataCoding(Data.DFLT_DATA_CODING);
		request.setShortMessage(shortMessage, Data.ENC_CP1252);
	}

	protected Pattern getDestinationPattern() {
		return Pattern.compile("^\\d{9,}$");
	}
}