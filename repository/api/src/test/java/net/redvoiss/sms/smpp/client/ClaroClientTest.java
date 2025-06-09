package net.redvoiss.sms.smpp.client;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ClaroClientTest {
    protected static final String[] VALID_INPUT = {
            "id:439906790520 sub:001 dlvrd:001 submit date:1708221224 done date:1708221224 stat:DELIVRD err:000 text:DLR" };

    @Test
    public void test() throws Exception {
        for (String i : VALID_INPUT) {
            assertEquals("666C7CEC78", Long.toHexString(Long.parseLong(SMPPClient.getDLRInfo(i).getId())).toUpperCase());
        }
    }
}