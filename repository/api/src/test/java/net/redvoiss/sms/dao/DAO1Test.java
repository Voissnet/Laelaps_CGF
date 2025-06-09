package net.redvoiss.sms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import net.redvoiss.sms.bean.Message;
import net.redvoiss.sms.dao.DAO.ReportState;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author Jorge Avila
 */
@Ignore
public class DAO1Test {

    private static final Logger LOGGER = Logger.getLogger(DAO1Test.class.getName());
    private static DataSource DS = null;
    private static final String MSGID_GW = "msgid_gw", IMSI = "IMSI";
    private String codeSmsc;
    private static final int ITERATOR = 1, SLOT = 0;

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
            try (PreparedStatement ps = conn.prepareStatement("insert into FS_SMSCGW (MSGID, SENDER, SOURCEADDRESS, DSTADDRESS, MESSAGE, RECEPTION_DATE, ID_REPORT) values (?, ?, ?, ?, ?, sysdate, ?)")) {
                ps.setString(1, MSGID_GW);
                final String toString = UUID.randomUUID().toString();
                ps.setString(2, toString);
                ps.setString(3, toString);
                ps.setString(4, toString);
                ps.setString(5, toString);
                ps.setString(6, ReportState.INITIAL.getState());
                int q = ps.executeUpdate();
                assert q > 0;
            }
            try (PreparedStatement ps = conn.prepareStatement("insert into FS_SMS_TELES (IMSI, REAL_NUMBER, RUTA_COD, SEND, DELIVERED_COUNT, SLOT) values (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, IMSI);
                final String toString = UUID.randomUUID().toString();
                ps.setString(2, toString);
                ps.setInt(3, 0);
                ps.setInt(4, 1);
                ps.setInt(5, 0);
                ps.setInt(6, SLOT);
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
            try (PreparedStatement ps = conn.prepareStatement("delete from FS_SMSCGW where MSGID=?")) {
                ps.setString(1, MSGID_GW);
                int q = ps.executeUpdate();
                assert q > 0 : String.format("Unable to delete FS_SMSCGW record: %s", MSGID_GW);
            }
            try (PreparedStatement ps = conn.prepareStatement("delete from FS_SMS_TELES where IMSI=?")) {
                ps.setString(1, IMSI);
                int q = ps.executeUpdate();
                assert q > 0 : String.format("Unable to delete FS_SMS_TELES record: %s", IMSI);
            }
        }
    }

    @Test
    public void cleanFaultyRecordTest() throws SQLException {
        try (Connection conn = DS.getConnection()) {
            List<Message> aList = DAO.findMessage(conn, codeSmsc, ITERATOR);
            assertNotNull("List is null", aList);
            assertFalse("List is empty", aList.isEmpty());
            assertEquals("Unexpected size", 1, aList.size());
            final Message message = aList.get(0);
            assertEquals(MSGID_GW, message.getSmscGWId());
            boolean isCleanFaultyRecordOk = DAO.cleanFaultyRecord(conn, message.getSmsCode(), message.getSmscGWId());
            assertTrue(isCleanFaultyRecordOk);
        }
    }

    @Test
    public void storeSuccessRecordTest() throws SQLException {
        try (Connection conn = DS.getConnection()) {
            List<Message> aList = DAO.findMessage(conn, codeSmsc, ITERATOR);
            assertNotNull("List is null", aList);
            assertFalse("List is empty", aList.isEmpty());
            assertEquals("Unexpected size", 1, aList.size());
            final Message message = aList.get(0);
            boolean isStoreSuccessRecord = DAO.storeSuccessRecord(conn, message.getSmsCode(), message.getSmscGWId(), SLOT, new Date());
            assertTrue(isStoreSuccessRecord);
        }
    }

    @Test
    public void actualizaSMSGWTest() throws SQLException {
        try (Connection conn = DS.getConnection()) {
            List<Message> aList = DAO.findMessage(conn, codeSmsc, ITERATOR);
            assertNotNull("List is null", aList);
            assertFalse("List is empty", aList.isEmpty());
            assertEquals("Unexpected size", 1, aList.size());
            final Message message = aList.get(0);
            assertNotNull(message);
            boolean isActualizaSMSGWOk = DAO.actualizaSMSGW(conn, MSGID_GW, ReportState.INITIAL);
            assert isActualizaSMSGWOk;
        }
    }

    @Test
    public void storeAbandonedRecordTest() throws SQLException {
        try (Connection conn = DS.getConnection()) {
            List<Message> aList = DAO.findMessage(conn, codeSmsc, ITERATOR);
            assertNotNull("List is null", aList);
            assertFalse("List is empty", aList.isEmpty());
            assertEquals("Unexpected size", 1, aList.size());
            final Message message = aList.get(0);
            assertNotNull(message);
            boolean isStoreAbandonedRecordOk = DAO.storeAbandonedRecord(conn, message.getSmsCode(), MSGID_GW, SLOT, new Date());
            assert isStoreAbandonedRecordOk;
        }
    }

    @Test
    public void storeFaultyRecordTest() throws SQLException {
        try (Connection conn = DS.getConnection()) {
            List<Message> aList = DAO.findMessage(conn, codeSmsc, ITERATOR);
            assertNotNull("List is null", aList);
            assertFalse("List is empty", aList.isEmpty());
            assertEquals("Unexpected size", 1, aList.size());
            final Message message = aList.get(0);
            assertNotNull(message);
            boolean isStoreFaultyRecordOk = DAO.storeFaultyRecord(conn, message.getSmsCode(), MSGID_GW, SLOT);
            assert isStoreFaultyRecordOk;
        }
    }
}
