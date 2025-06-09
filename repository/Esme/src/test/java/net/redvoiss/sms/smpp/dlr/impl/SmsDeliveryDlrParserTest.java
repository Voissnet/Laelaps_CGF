package net.redvoiss.sms.smpp.dlr.impl;

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
public class SmsDeliveryDlrParserTest {
    /**
     *
     * @throws ParseException
     */
    @Test
    public void test() throws ParseException {
        SmsDeliveryDlrParser dlr = new SmsDeliveryDlrParser("id:6f2929a9-e61a-435f-9c7a-55ccd3f26a7b submit date:201902261755 done date:201902261756 stat:ACCEPTD err:006");
        final Optional<String> id = dlr.parseIdField();
        assertTrue(id.isPresent());
        assertEquals("6f2929a9-e61a-435f-9c7a-55ccd3f26a7b", id.get());
        final Optional<Date> submitDate = dlr.parseSubmitDateField();
        assertTrue(submitDate.isPresent());
        final SimpleDateFormat simpleDateFormat = dlr.getSimpleDateFormat();
        assertEquals("201902261755", simpleDateFormat.format(submitDate.get()));
        final Optional<Date> doneDate = dlr.parseDoneDateField();
        assertTrue(doneDate.isPresent());
        assertEquals("201902261756", simpleDateFormat.format(doneDate.get()));
        final Optional<DlrParser.StatEnum> stat = dlr.parseStatField();
        assertTrue(stat.isPresent());
        assertEquals(DlrParser.StatEnum.ACCEPTD, stat.get());
        final Optional<String> err = dlr.parseErrField();
        assertTrue(err.isPresent());
        assertEquals("006", err.get());
    }    
}
