package org.smpp.client;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.smpp.pdu.SubmitSM;
import org.smpp.Data;

public class SMPPSenderTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public SMPPSenderTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( SMPPSenderTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
		SubmitSM request = new SubmitSM();
		request.setRegisteredDelivery(org.smpp.Data.SM_SMSC_RECEIPT_REQUESTED);
		byte registeredDelivery = (byte) (request.getRegisteredDelivery() & Data.SM_SMSC_RECEIPT_MASK);
		assertEquals( Data.SM_SMSC_RECEIPT_REQUESTED, registeredDelivery );
    }
}
