package net.redvoiss.sms.smpp.client;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class SMPPClientTest {
    @Test
    public void test() throws Exception {
        String release = SMPPClient.getRelease();
        assertNotNull( release );
        assertFalse( release.isEmpty() );
        assertNotEquals( release, "Unable to read release details");
        System.out.println( release );
    }
}