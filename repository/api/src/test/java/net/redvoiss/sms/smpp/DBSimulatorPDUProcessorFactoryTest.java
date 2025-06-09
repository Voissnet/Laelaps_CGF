package net.redvoiss.sms.smpp;

import java.io.IOException;
import java.util.logging.Logger;
import org.junit.Test;
import org.smpp.smscsim.PDUProcessor;
import org.smpp.smscsim.PDUProcessorFactory;

import static org.junit.Assert.assertNotNull;
import org.smpp.Connection;
import org.smpp.pdu.PDU;
import org.smpp.pdu.PDUException;
import org.smpp.smscsim.PDUProcessorGroup;
import org.smpp.smscsim.SMSCSession;
import org.smpp.util.ByteBuffer;

import static java.util.logging.Level.SEVERE;
import org.junit.BeforeClass;

public class DBSimulatorPDUProcessorFactoryTest {

    private static final Logger LOGGER = Logger.getLogger(DBSimulatorPDUProcessorFactoryTest.class.getName());

    private static PDUProcessorFactory pduProcessorFactory;

    @BeforeClass
    public static void init() {
        pduProcessorFactory = new DBSimulatorPDUProcessorFactory(null, null, new PDUProcessorGroup() {
        }, null, null, null);
        assertNotNull(pduProcessorFactory);
    }

    @Test
    public void test() {
        PDUProcessor pduProcessor = pduProcessorFactory.createPDUProcessor(
                new SMSCSession() {
            @Override
            public void stop() {
                LOGGER.info("Stops");
            }

            @Override
            public void run() {
                throw new UnsupportedOperationException("run() Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void send(PDU pdu) throws IOException, PDUException {
                throw new UnsupportedOperationException("send(PDU pdu) Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void setPDUProcessor(PDUProcessor pduProcessor) {
                throw new UnsupportedOperationException("setPDUProcessor(PDUProcessor pduProcessor) Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void setPDUProcessorFactory(PDUProcessorFactory pduProcessorFactory) {
                throw new UnsupportedOperationException("setPDUProcessorFactory(PDUProcessorFactory pduProcessorFactory) Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void setReceiveTimeout(long timeout) {
                throw new UnsupportedOperationException("setReceiveTimeout(long timeout) Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public long getReceiveTimeout() {
                throw new UnsupportedOperationException("getReceiveTimeout() Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Object getAccount() {
                throw new UnsupportedOperationException("getAccount() Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void setAccount(Object account) {
                throw new UnsupportedOperationException("setAccount(Object account) Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Connection getConnection() {
                return new Connection() {
                    @Override
                    public void open() throws IOException {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }

                    @Override
                    public void close() throws IOException {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }

                    @Override
                    public boolean isOpened() {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }

                    @Override
                    public void send(ByteBuffer data) throws IOException {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }

                    @Override
                    public ByteBuffer receive() throws IOException {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }

                    @Override
                    public Connection accept() throws IOException {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }
                };
            }
        }
        );
        assertNotNull(pduProcessor);
        try {
            pduProcessor.exit();
        } catch (Exception e) {
            LOGGER.log(SEVERE, "Unexpected exception", e);
        }
    }
}
