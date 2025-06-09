package net.redvoiss.sms.smpp.dlr;

import net.redvoiss.sms.smpp.dlr.impl.TeltechDlrParser;
import net.redvoiss.sms.smpp.dlr.impl.SmsDeliveryDlrParser;
import java.text.ParseException;
import java.util.Locale;
import java.util.Optional;
import net.redvoiss.sms.smpp.dlr.DlrParser.StatEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Jorge Avila
 */
public class DLRTest {

    /**
     *
     * @throws ParseException
     */
    @Test
    public void testEmptyDlr() throws ParseException {
        DlrParser dlr = new SmsDeliveryDlrParser(null);
        assertFalse(dlr.parseDlvrdField().isPresent());
        assertFalse(dlr.parseDoneDateField().isPresent());
        assertFalse(dlr.parseErrField().isPresent());
        assertFalse(dlr.parseIdField().isPresent());
        assertFalse(dlr.parseStatField().isPresent());
        assertFalse(dlr.parseSubField().isPresent());
        assertFalse(dlr.parseSubmitDateField().isPresent());
        assertFalse(dlr.parseTextField().isPresent());
    }

    /**
     * Tests multi line DLR
     */
    @Test
    public void testMultilineDlr() {
        DlrParser dlr = new SmsDeliveryDlrParser("id:15620edc sub:001 dlvrd:001 submit date:1805091830 done date:1805091830 stat:DELIVRD err:000 text:Hola:\n?como va el t");
        final Optional<String> id = dlr.parseIdField();
        assertTrue(id.isPresent());
        assertEquals("15620edc", id.get());
    }

    /**
     * Tests particular format
     */
    @Test
    public void testEntelDlr() {
        DlrParser dlr = new SmsDeliveryDlrParser("id:152ec848 sub:001 dlvrd:001 submit date:1707041602 done date:1707041602 stat:DELIVRD err:000 text:???El c?digo de Adob");
        final Optional<String> id = dlr.parseIdField();
        assertTrue(id.isPresent());
        assertEquals("152ec848", id.get());
    }

    /**
     * Test particular DLR format
     */
    @Test
    public void testClaroDlr() {
        DlrParser dlr = new SmsDeliveryDlrParser("id:439906790520 sub:001 dlvrd:001 submit date:1708221224 done date:1708221224 stat:DELIVRD err:000 text:DLR");
        final Optional<String> id = dlr.parseIdField();
        assertTrue(id.isPresent());
        assertEquals("666C7CEC78", Long.toHexString(Long.parseLong(id.get())).toUpperCase(Locale.US));
    }

    /**
     * Tests particular DLR format
     */
    @Test
    public void testSmsDeliveryDlr() {
        DlrParser dlr = new SmsDeliveryDlrParser("id:4e05c166-6ced-40df-9e1d-2bbcb0c14cd3 submit date:201907191552 done date:201907191552 stat:UNDELIV err:005");
        final Optional<StatEnum> stat = dlr.parseStatField();
        assertTrue(stat.isPresent());
        assertEquals(StatEnum.UNDELIV, stat.get());
    }

    /**
     * Tests particular DLR format
     */
    @Test
    public void testTeltechDlr() {
        DlrParser dlr = new TeltechDlrParser("id:ae095944-07fc-4162-8004-c5549761445a sub:001 dlvrd:001 submit date:1907241442 done date:1907241457 stat:DELIVRD err:002 text:Text:Estimado cliente de 5");
        final Optional<StatEnum> stat = dlr.parseStatField();
        assertTrue(stat.isPresent());
        assertEquals(StatEnum.DELIVRD, stat.get());
    }

}
