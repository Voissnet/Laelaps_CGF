package net.redvoiss.sms.smpp.client;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class EntelClientTest {
    protected static final String[] VALID_INPUT = {
            "id:15620edc sub:001 dlvrd:001 submit date:1805091830 done date:1805091830 stat:DELIVRD err:000 text:Hola:\n?como va el t",
            "id:152ec848 sub:001 dlvrd:001 submit date:1707041602 done date:1707041602 stat:DELIVRD err:000 text:???El c?digo de Adob" };
    protected static final String[] EXPECTED_ID_OUTPUT = { "15620edc", "152ec848" };
    protected static final StatEnum[] EXPECTED_STAT_OUTPUT = { StatEnum.DELIVRD, StatEnum.DELIVRD };

    @Test
    public void test() throws Exception {
        for (int i = 0; i < VALID_INPUT.length; i++) {
            assertEquals(EXPECTED_ID_OUTPUT[i], SMPPClient.getDLRInfo(VALID_INPUT[i]).getId());
        }
    }

    @Test
    public void testNullInput() throws Exception {
        assertNull(SMPPClient.getDLRInfo(null));
    }

    @Test
    public void testInvalid() {
        Pattern pattern = Pattern.compile("^id:(\\S+).*", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(VALID_INPUT[0]);
        assertFalse(matcher.matches() && matcher.groupCount() > 0);
    }

    @Test
    public void testStat() {
        for (int i = 0; i < VALID_INPUT.length; i++) {
            assertEquals(EXPECTED_STAT_OUTPUT[i], SMPPClient.getDLRInfo(VALID_INPUT[i]).getStat());
        }
    }
}