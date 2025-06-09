package net.redvoiss.sms.smpp.dlr.impl;

import java.text.SimpleDateFormat;
import net.redvoiss.sms.smpp.dlr.AbstractDlrParserImpl;

/**
 *
 * @author Jorge Avila
 */
public class SmsDeliveryDlrParser extends AbstractDlrParserImpl {

    private final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmm");

    /**
     * Creates DLR parser
     *
     * @param dlr
     */
    public SmsDeliveryDlrParser(String dlr) {
        super(dlr);
    }

    /**
     * Establishes concrete date format
     *
     * @return
     */
    @Override
    protected SimpleDateFormat getSimpleDateFormat() {
        return SIMPLE_DATE_FORMAT;
    }

}
