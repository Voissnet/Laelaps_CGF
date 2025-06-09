package net.redvoiss.sms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import net.redvoiss.sms.bean.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author Jorge Avila
 */
@Ignore
public class DAO0Test {

    private static final Logger LOGGER = Logger.getLogger(DAO0Test.class.getName());
    private static DataSource DS = null;
    private static final String MSGID_GW = "msgid_gw";
    private String codeSmsc;
    private static final int ITERATOR = 1;

    @BeforeClass
    public static void onlyOnce() {
        try {
            DS = DAO.buildLegacyOracleDataSource();
        } catch (SQLException ex) {
            DS = null;
            LOGGER.log(SEVERE, "Unexpected SQLException while setting up data source", ex);
        } finally {
            assertNotNull(DS);
        }
    }

    @Before
    public void initializeRecord() throws SQLException {
        LOGGER.fine("Setting up record");
        try (Connection conn = DS.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("insert into FS_ESME (SMS_COD, PRIOR_COD, CODE_RUTA, MSGID_GW, code_smsc, iterator, fecha_hora) values (?, ?, ?, ?, ?, ?, sysdate)")) {
                ps.setString(1, "THIS WILL BE OVERRIDEN BY FG.TRIG_FS_ESME");// This value gets overriden by FG.TRIG_FS_ESME
                ps.setInt(2, 0);
                ps.setInt(3, 0);
                ps.setString(4, MSGID_GW);
                codeSmsc = UUID.randomUUID().toString();
                LOGGER.log(FINE, "Initializing record using {0}", codeSmsc);
                ps.setString(5, codeSmsc);
                ps.setInt(6, ITERATOR);
                int q = ps.executeUpdate();
                assert q > 0;
            }
        } catch (Exception ex) {
            LOGGER.log(SEVERE, "Unexpected exception", ex);
            throw ex;
        }
    }

    @After
    public void disposeRecord() throws SQLException {
        LOGGER.fine("Dispossing record");
        try (Connection conn = DS.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("delete from FS_ESME where code_smsc=?")) {
                ps.setString(1, codeSmsc);
                int q = ps.executeUpdate();
                assert q > 0 : String.format("Unable to delete %s", codeSmsc);
            }
        }
    }

    @Test
    public void findMessageTest() throws SQLException {
        try (Connection conn = DS.getConnection()) {
            List<Message> aList = DAO.findMessage(conn, codeSmsc, ITERATOR);
            assertNotNull("List is null", aList);
            assertFalse("List is empty", aList.isEmpty());
            assertEquals("Unexpected size", 1, aList.size());
            final Message message = aList.get(0);
            assertNotNull(message);
            //Record identifier gets overriden by the DB.
            //There is no use to try to try to verify its value
            assertEquals(MSGID_GW, message.getSmscGWId());
        }
    }

    @Test
    public void sentTest() throws SQLException {
        try (Connection conn = DS.getConnection()) {
            List<Message> aList = DAO.findMessage(conn, codeSmsc, ITERATOR);
            assertNotNull("List is null", aList);
            assertFalse("List is empty", aList.isEmpty());
            assertEquals("Unexpected size", 1, aList.size());
            final Message message = aList.get(0);
            assertNotNull(message);
            int q = DAO.sent(conn, MSGID_GW, message.getSmsCode());
            assert q > 0;
            codeSmsc = MSGID_GW;
        }
    }

    @Test
    public void storeSequenceNumberTest() throws SQLException {
        try (Connection conn = DS.getConnection()) {
            List<Message> aList = DAO.findMessage(conn, codeSmsc, ITERATOR);
            assertNotNull("List is null", aList);
            assertFalse("List is empty", aList.isEmpty());
            assertEquals("Unexpected size", 1, aList.size());
            final Message message = aList.get(0);
            assertNotNull(message);
            int q = DAO.storeSequenceNumber(conn, message.getSmsCode(), 2357);
            assert q > 0;
        }
    }
}
