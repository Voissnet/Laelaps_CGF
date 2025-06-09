package net.redvoiss.sms.smpp.client;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class MovistarClientTest {
    @Test
    public void test() throws Exception {
        java.util.regex.Matcher contentMatcher = MovistarClient.MOBILE_NUMBER_PATTERN.matcher("56968640945");
        assertTrue( contentMatcher.matches() );
    }
}