package net.redvoiss.sms.smpp.dlr.impl;

import java.text.SimpleDateFormat;
import net.redvoiss.sms.smpp.dlr.AbstractDlrParserImpl;

/**
 *
 * @author Jorge Avila
 */
public class TeltechDlrParser extends AbstractDlrParserImpl {

    private final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyMMddHHmm");

    /**
     * Creates DLR parser
     *
     * @param dlr
     */
    public TeltechDlrParser(String dlr) {
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
