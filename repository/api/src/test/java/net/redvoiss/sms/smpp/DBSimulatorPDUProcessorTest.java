package net.redvoiss.sms.smpp;

import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Jorge Avila
 */
public class DBSimulatorPDUProcessorTest {

    private static final Logger LOGGER = Logger.getLogger(DBSimulatorPDUProcessorTest.class.getName());

    private static DBSimulatorPDUProcessor dbSimulatorPDUProcessor;

    @BeforeClass
    public static void init() {
        dbSimulatorPDUProcessor = new DBSimulatorPDUProcessor(null, null, null, null, null);
        assertNotNull(dbSimulatorPDUProcessor);
    }

    @Test
    public void test() {
        try {
            dbSimulatorPDUProcessor.run();
        } catch (Exception e) {
            LOGGER.log(SEVERE, "Unexpected exception", e);
        }
    }
}
