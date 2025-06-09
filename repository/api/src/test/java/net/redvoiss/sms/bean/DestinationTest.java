package net.redvoiss.sms.bean;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.regex.Pattern;

import net.redvoiss.sms.SMSException;

public class DestinationTest {
    private static final String[] INPUT_OK = {"+569977667505", "569977667505", "+56977667505", "56977667505", "+54997766750", "54997766750"};
    private static final String[] INPUT_FAULT = {"+0569776670", "01234", "+01234", "0", "01", "+0" };

    //private static final String[] INPUT_OK = {"+569994994578", "+56999499457", "56999499457", "+5699949457", "5999499457", "1", "+1"};
    //private static final String[] INPUT_FAULT = {"+56999499457", "01234", "+01234", "0", "01", "+0"};
    private static final Pattern DESTINATION_NUMBER_PATTERN = Pattern.compile("^\\d+$");

    @Test
    public void testOK() throws SMSException {
        for (String s : INPUT_OK) {
            Destination d = new Destination(s);
            assertTrue(d.toString(), d.isOK());
            assertTrue(d.getTarget(), DESTINATION_NUMBER_PATTERN.matcher(d.getTarget()).matches());
        }
    }

    @Test
    public void testFault() throws SMSException {
        for (String s : INPUT_FAULT) {
            Destination d = new Destination(s);
            assertFalse(d.toString(), d.isOK());
        }
    }
    
    @Test
    public void testOK_v2() throws SMSException {
        // CGF 2020912. I check with isOK_v2 function
        for (String s : INPUT_OK) {
            Destination d = new Destination(s);
            assertTrue(d.toString(), d.isOK_v2());
            assertTrue(d.getTarget(), DESTINATION_NUMBER_PATTERN.matcher(d.getTarget()).matches());
        }
    }

    @Test
    public void testFault_v2() throws SMSException {
        // CGF 2020912. I check with isOK_v2 function
        for (String s : INPUT_FAULT) {
            Destination d = new Destination(s);
            assertFalse(d.toString(), d.isOK_v2());
        }
    }
}
