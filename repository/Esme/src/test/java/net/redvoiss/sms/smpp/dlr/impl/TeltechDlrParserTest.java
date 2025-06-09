package net.redvoiss.sms.smpp.dlr.impl;

import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import net.redvoiss.sms.smpp.dlr.DlrParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Jorge Avila
 */
public class TeltechDlrParserTest {

    /**
     *
     * @throws ParseException
     */
    @Test
    public void testTeltechDlrMultiline() throws ParseException {
        String input = new String(new byte[]{(byte) 0x69, (byte) 0x64, (byte) 0x3a, (byte) 0x37, (byte) 0x62, (byte) 0x38, (byte) 0x34, (byte) 0x65, (byte) 0x32, (byte) 0x31, (byte) 0x32, (byte) 0x2d, (byte) 0x38, (byte) 0x33, (byte) 0x30, (byte) 0x32, (byte) 0x2d, (byte) 0x34, (byte) 0x37, (byte) 0x33, (byte) 0x65, (byte) 0x2d, (byte) 0x39, (byte) 0x31, (byte) 0x31, (byte) 0x30, (byte) 0x2d, (byte) 0x64, (byte) 0x38, (byte) 0x61, (byte) 0x38, (byte) 0x39, (byte) 0x62, (byte) 0x37, (byte) 0x64, (byte) 0x64, (byte) 0x36, (byte) 0x64, (byte) 0x61, (byte) 0x20, (byte) 0x73, (byte) 0x75, (byte) 0x62, (byte) 0x3a, (byte) 0x30, (byte) 0x30, (byte) 0x31, (byte) 0x20, (byte) 0x64, (byte) 0x6c, (byte) 0x76, (byte) 0x72, (byte) 0x64, (byte) 0x3a, (byte) 0x30, (byte) 0x30, (byte) 0x31, (byte) 0x20, (byte) 0x73, (byte) 0x75, (byte) 0x62, (byte) 0x6d, (byte) 0x69, (byte) 0x74, (byte) 0x20, (byte) 0x64, (byte) 0x61, (byte) 0x74, (byte) 0x65, (byte) 0x3a, (byte) 0x31, (byte) 0x39, (byte) 0x30, (byte) 0x37, (byte) 0x32, (byte) 0x34, (byte) 0x31, (byte) 0x35, (byte) 0x33, (byte) 0x34, (byte) 0x20, (byte) 0x64, (byte) 0x6f, (byte) 0x6e, (byte) 0x65, (byte) 0x20, (byte) 0x64, (byte) 0x61, (byte) 0x74, (byte) 0x65, (byte) 0x3a, (byte) 0x31, (byte) 0x39, (byte) 0x30, (byte) 0x37, (byte) 0x32, (byte) 0x34, (byte) 0x31, (byte) 0x35, (byte) 0x33, (byte) 0x35, (byte) 0x20, (byte) 0x73, (byte) 0x74, (byte) 0x61, (byte) 0x74, (byte) 0x3a, (byte) 0x44, (byte) 0x45, (byte) 0x4c, (byte) 0x49, (byte) 0x56, (byte) 0x52, (byte) 0x44, (byte) 0x20, (byte) 0x65, (byte) 0x72, (byte) 0x72, (byte) 0x3a, (byte) 0x30, (byte) 0x30, (byte) 0x32, (byte) 0x20, (byte) 0x74, (byte) 0x65, (byte) 0x78, (byte) 0x74, (byte) 0x3a, (byte) 0x54, (byte) 0x65, (byte) 0x78, (byte) 0x74, (byte) 0x3a, (byte) 0x49, (byte) 0x52, (byte) 0x45, (byte) 0x4e, (byte) 0x45, (byte) 0x2c, (byte) 0x0d, (byte) 0x0a, (byte) 0x20, (byte) 0x63, (byte) 0x6f, (byte) 0x6d, (byte) 0x6f, (byte) 0x20, (byte) 0x63, (byte) 0x6c, (byte) 0x69, (byte) 0x65, (byte) 0x6e, (byte) 0x74, (byte) 0x35}, Charset.defaultCharset());
        //System.out.println( input );
        TeltechDlrParser dlr = new TeltechDlrParser(input);
        final Optional<DlrParser.StatEnum> stat = dlr.parseStatField();
        assertTrue(stat.isPresent());
        assertEquals(DlrParser.StatEnum.DELIVRD, stat.get());
        final Optional<Date> submitDate = dlr.parseSubmitDateField();
        assertTrue(submitDate.isPresent());
        final SimpleDateFormat simpleDateFormat = dlr.getSimpleDateFormat();
        assertEquals("1907241534", simpleDateFormat.format(submitDate.get()));
        final Optional<Date> doneDate = dlr.parseDoneDateField();
        assertTrue(doneDate.isPresent());
        assertEquals("1907241535", simpleDateFormat.format(doneDate.get()));
        final Optional<String> err = dlr.parseErrField();
        assertTrue(err.isPresent());
        assertEquals("002", err.get());
    }
}
