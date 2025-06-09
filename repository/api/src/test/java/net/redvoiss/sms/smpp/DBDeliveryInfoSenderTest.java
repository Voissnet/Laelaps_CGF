package net.redvoiss.sms.smpp;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DBDeliveryInfoSenderTest {
    @Test
    public void testIncomingDestination() throws Exception {
        assertEquals("310215448909000", DBDeliveryInfoSender.processDestination("215310448909000"));
    }
}