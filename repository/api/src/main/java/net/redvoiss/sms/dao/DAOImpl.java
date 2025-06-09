package net.redvoiss.sms.dao;

import net.redvoiss.sms.bean.NewMessage;
import net.redvoiss.sms.bean.PendingMessage;
import net.redvoiss.sms.bean.Slot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

public class DAOImpl implements DAO {

    private final DataSource m_ds;

    public DAOImpl(DataSource ds) {
        m_ds = ds;
    }

    @Override
    public boolean increaseRetry(String smsCode) throws SQLException {
        boolean ret = false;
        final String propertyName = "net.redvoiss.sms.dao.retry";
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            try (Connection c = m_ds.getConnection()) {
                try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                    LOGGER.log(FINE, propertyName, new Object[]{smsCode});
                    s.setString(1, smsCode);
                    int rowsAffected = s.executeUpdate();
                    ret = rowsAffected > 0;
                }
            }
        }
        return ret;
    }

    @Override
    public boolean markLost(String smsCode) throws SQLException {
        boolean ret = false;
        final String propertyName = "net.redvoiss.sms.dao.lost";
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            try (Connection c = m_ds.getConnection()) {
                try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                    LOGGER.log(FINE, propertyName, new Object[]{smsCode});
                    s.setString(1, MessageState.NEW.getState());
                    s.setString(2, smsCode);
                    int rowsAffected = s.executeUpdate();
                    ret = rowsAffected > 0;
                }
            }
        }
        return ret;
    }

    @Override
    public boolean enqueued(String gsmCode, String smsCode) throws SQLException {
        try (Connection c = m_ds.getConnection()) {
            return DAO.enqueued(c, gsmCode, smsCode) > 0;
        }
    }

    @Override
    public boolean sent(String gsmCode, String smsCode, Date sentDate) throws SQLException {
        try (Connection c = m_ds.getConnection()) {
            return DAO.sent(c, gsmCode, smsCode, sentDate) > 0;
        }
    }

    @Override
    public boolean register_sent2(String gsmCode, String smsCode) throws SQLException {
        try (Connection c = m_ds.getConnection()) {
            return DAO.register_sent2(c, gsmCode, smsCode) > 0;
        }
    }
    
    @Override
    public boolean markRecordReadyToBeProcessed(String smsCode, String gsmCode, String sourceAddress)
            throws SQLException {
        boolean ret = false;
        final String propertyName = "net.redvoiss.sms.dao.teles.message_ok";
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            try (Connection c = m_ds.getConnection()) {
                try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                    LOGGER.log(FINE, propertyName, new Object[]{smsCode, gsmCode, sourceAddress});
                    s.setString(1, gsmCode);
                    s.setString(2, sourceAddress);
                    s.setString(3, smsCode);
                    int rowsAffected = s.executeUpdate();
                    ret = rowsAffected > 0;
                }
            }
        }
        return ret;
    }

    @Override
    public Map<String, ImsiRecord> getRealNumberFromIMSI(int route) throws SQLException {
        Map<String, ImsiRecord> ret = new HashMap<>();
        final String propertyName = "net.redvoiss.sms.dao.reply.imsi";
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            try (Connection c = m_ds.getConnection()) {
                try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                    s.setInt(1, route);
                    try (ResultSet rs = s.executeQuery()) {
                        while (rs.next()) {
                            final String imsi = rs.getString("IMSI");
                            final String number = rs.getString("REAL_NUMBER");
                            final int slot = rs.getInt("SLOT");
                            final boolean enabled = rs.getBoolean("SEND");
                            ImsiRecord ir = new ImsiRecord(imsi, number, slot, enabled);
                            LOGGER.log(FINE, "net.redvoiss.sms.dao.reply.imsi", new Object[]{imsi, number});
                            ret.put(imsi, ir);
                        }
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public void deleteRecord(String smsCode) throws SQLException {
        try (Connection c = m_ds.getConnection()) {
            int q = DAO.deleteSMS(c, smsCode);
            assert q > 0;
        }
    }

    @Override
    public void storeSuccessRecord(String smsCode, int channel, String idGW, Date confirmationDate)
            throws SQLException {
        try (Connection conn = m_ds.getConnection()) {
            boolean isOk = DAO.storeSuccessRecord(conn, smsCode, idGW, channel, confirmationDate);
            assert isOk;
        }
    }

    /*
     * @see https://docs.oracle.com/javase/tutorial/jdbc/basics/transactions.html
     * @see http://stackoverflow.com/questions/15761791/transaction-rollback-on-sqlexception-using-new-try-with-resources-block
     */
    @Override
    public void storeSuccessRecord(String smsCode, int channel, Date confirmationDate) throws SQLException {
        try (Connection c = m_ds.getConnection()) {
            boolean isOk = DAO.storeSuccessRecord(c, smsCode, null, channel, confirmationDate);
            assert isOk;
        }
    }

    @Override
    public void storeAbandonedRecord(String smsCode, int channel, Date confirmationDate) throws SQLException {
        try (Connection conn = m_ds.getConnection()) {
            boolean isOk = DAO.storeAbandonedRecord(conn, smsCode, null, channel, confirmationDate);
            assert isOk;
        }
    }

    @Override
    public void storeAbandonedRecord(String smsCode, int channel, String idGW, Date confirmationDate)
            throws SQLException {
        try (Connection conn = m_ds.getConnection()) {
            boolean isOk = DAO.storeAbandonedRecord(conn, smsCode, idGW, channel, confirmationDate);
            assert isOk;
        }
    }

    @Override
    public void storeFaultyRecord(String smsCode, int channel) throws SQLException {
        try (Connection c = m_ds.getConnection()) {
            boolean isOk = DAO.storeFaultyRecord(c, smsCode, null, channel);
            assert isOk;
        }
    }

    @Override
    public void cleanFaultyRecord(String smsCode) throws SQLException {
        try (Connection c = m_ds.getConnection()) {
            boolean isOK = DAO.cleanFaultyRecord(c, smsCode, null);
            assert isOK;
        }
    }

    @Override
    public void storeFaultyRecord(String smsCode, String idGW, int channel) throws SQLException {
        try (Connection conn = m_ds.getConnection()) {
            boolean isOk = DAO.storeFaultyRecord(conn, smsCode, idGW, channel);
            assert isOk;
        }
    }

    @Override
    public void cleanFaultyRecord(String smsCode, String idGW) throws SQLException {
        try (Connection conn = m_ds.getConnection()) {
            boolean isOK = DAO.cleanFaultyRecord(conn, smsCode, idGW);
            assert isOK;
        }
    }

    /**
     *
     * @param route
     * @param quantity
     * @return
     * @throws SQLException
     */
    @Override
    public List<NewMessage> getSMSList(int route, int quantity) throws SQLException {
        try (Connection c = m_ds.getConnection()) {
            return DAO.getSMSList(c, route, quantity);
        }
    }

    @Override
    public List<PendingMessage> getPendingSMSList(int route) throws SQLException {
        List<PendingMessage> ret = new ArrayList<>();
        final String propertyName = "net.redvoiss.sms.dao.check.lingering_sms";
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            try (Connection c = m_ds.getConnection()) {
                try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                    s.setInt(1, route);
                    s.setString(2, MessageState.NEW.getState());
                    try (ResultSet rs = s.executeQuery()) {
                        while (rs.next()) {
                            final String smsCode = rs.getString("SMS_COD");
                            final String msgIdGW = rs.getString("MSGID_GW");
                            final String gsmId = rs.getString("CODE_SMSC");
                            final String destination = rs.getString("DST_ADDRS");
                            final String source = rs.getString("SOURC_ADDRS");
                            final String retry = rs.getString("RETRY");
                            assert route == rs.getInt("CODE_RUTA");
                            final Date date = new Date(rs.getDate("FECHA_HORA").getTime());
                            PendingMessage m = new PendingMessage(smsCode, msgIdGW, gsmId, destination, source, route,
                                    retry, date);
                            ret.add(m);
                        }
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public List<Slot> getAvailableSlot(int route) throws SQLException {
        List<Slot> ret = new ArrayList<>();
        final String propertyName = "net.redvoiss.sms.dao.check.slot";
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            try (Connection c = m_ds.getConnection()) {
                try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                    s.setInt(1, route);
                    s.setInt(2, 1);
                    try (ResultSet rs = s.executeQuery()) {
                        while (rs.next()) {
                            final String imsi = rs.getString("IMSI");
                            final String realNumber = rs.getString("REAL_NUMBER");
                            final int slotId = rs.getInt("SLOT");
                            Slot slot = new Slot(slotId, realNumber, imsi);
                            LOGGER.log(FINE, "net.redvoiss.sms.dao.reply.imsi", new Object[]{slot});
                            ret.add(slot);
                        }
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public void storeReply(String msgid, String sender, int route, String source, String destination, String message,
            Date receptionDate) throws SQLException {
        final String propertyName = "net.redvoiss.sms.dao.reply.record";
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            try (Connection c = m_ds.getConnection()) {
                try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                    s.setString(1, msgid);
                    s.setString(2, sender);
                    s.setInt(3, route);
                    s.setString(4, source);
                    s.setString(5, destination);
                    s.setString(6, message);
                    s.setDate(7, new java.sql.Date(receptionDate.getTime()));
                    s.executeUpdate();
                }
            }
        }
    }

}
