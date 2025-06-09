package net.redvoiss.sms.dao;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import net.redvoiss.sms.bean.Message;
import net.redvoiss.sms.bean.NewMessage;
import net.redvoiss.sms.bean.PendingMessage;
import net.redvoiss.sms.bean.Slot;

import oracle.jdbc.OracleConnection;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import javax.sql.rowset.serial.SerialBlob;

public interface DAO {

    static final Properties SQL_PROPERTIES = DAO.loadProperties();
    static final DateFormat DATE_FORMATTER = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    static final String PROPERTY_PATH = "net/redvoiss/sms/dao/sql.properties";
    static final Logger LOGGER = Logger.getLogger(DAO.class.getName(), DAO.class.getName());
    static final String SMS_SERVER = System.getProperty("sms_server","SMS");
    
    List<NewMessage> getSMSList(int route, int quantity) throws SQLException;

    List<PendingMessage> getPendingSMSList(int route) throws SQLException;

    boolean enqueued(String gsmCode, String smsCode) throws SQLException;

    boolean sent(String gsmCode, String smsCode, Date sentDate) throws SQLException;
    boolean register_sent2(String messageId, String smsCode)  throws SQLException;

    void storeSuccessRecord(String smsCode, int channel, Date confirmationDate) throws SQLException;

    void storeSuccessRecord(String smsCode, int channel, String idGW, Date confirmationDate) throws SQLException;

    void storeAbandonedRecord(String smsCode, int channel, Date confirmationDate) throws SQLException;

    void storeAbandonedRecord(String smsCode, int channel, String idGW, Date confirmationDate) throws SQLException;

    void storeFaultyRecord(String smsCode, int channel) throws SQLException;

    void storeFaultyRecord(String smsCode, String idGW, int channel) throws SQLException;

    void cleanFaultyRecord(String smsCode) throws SQLException;

    void cleanFaultyRecord(String smsCode, String idGW) throws SQLException;

    boolean increaseRetry(String smsCode) throws SQLException;

    void deleteRecord(String smsCode) throws SQLException;

    boolean markLost(String smsCode) throws SQLException;

    boolean markRecordReadyToBeProcessed(String smsCode, String gsmCode, String sourceAddress) throws SQLException;

    List<Slot> getAvailableSlot(int route) throws SQLException;

    Map<String, ImsiRecord> getRealNumberFromIMSI(int route) throws SQLException;

    class ImsiRecord {

        String imsi, number;
        int slot;
        boolean enabled;

        ImsiRecord(String imsi, String number, int slot, boolean enabled) {
            this.imsi = imsi;
            this.number = number;
            this.slot = slot;
            this.enabled = enabled;
        }

        public String getNumber() {
            return number;
        }

        public int getSlot() {
            return slot;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    void storeReply(String msgid, String sender, int route, String source, String destination, String message,
            Date receptionDate) throws SQLException;

    static javax.sql.DataSource buildOracleDataSource() throws SQLException {
        /**
         * Required Patch 21838827: MERGE REQUEST ON TOP OF 12.1.0.2.0 FOR BUGS
         * 19530366 21562478 https://community.oracle.com/thread/3935861
         */
        oracle.ucp.jdbc.PoolDataSource pds = oracle.ucp.jdbc.PoolDataSourceFactory.getPoolDataSource();
        pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
        Properties connprops = new Properties();
        pds.setURL("jdbc:oracle:thin:/@FONO");
        pds.setInitialPoolSize(1);
        pds.setMinPoolSize(5);
        pds.setMaxPoolSize(10);
        //--->
        String hostname = SMS_SERVER; //"SMS";
        try {
            hostname = java.net.InetAddress.getLocalHost().getCanonicalHostName();
        } catch (java.net.UnknownHostException uhe) {
            LOGGER.log(FINE, "Unexpected exception while getting FQDN", uhe);
        }
        String program = String.format("%s@%s", System.getProperty("program", "undefined"), hostname);
        LOGGER.fine(String.format("Program name to be used for DB session is: {%s}", program));
        connprops.put("v$session.program", program);
        //connprops.put("v$session.process", ProcessHandle.current().getPid() );//TODO Enable when J9 ready
        pds.setConnectionProperties(connprops);
        //https://blogs.oracle.com/WebLogicServer/entry/setting_v_session_for_a
        //--->
        pds.setValidateConnectionOnBorrow(true);
        pds.setSQLForValidateConnection("select 1 from dual");
        //https://docs.oracle.com/en/database/oracle/oracle-database/12.2/jjucp/validating-ucp-connections.html
        return pds;
    }

    @Deprecated
    static javax.sql.DataSource buildLegacyOracleDataSource() throws SQLException {
        java.sql.DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
        oracle.jdbc.pool.OracleDataSource ret = new oracle.jdbc.pool.OracleDataSource();
        java.util.Properties props = new java.util.Properties();
        props.put("v$session.program", SMS_SERVER);//https://blogs.oracle.com/WebLogicServer/entry/setting_v_session_for_a
        props.setProperty(OracleConnection.CONNECTION_PROPERTY_THIN_NET_CONNECT_TIMEOUT, "2000");//https://stackoverflow.com/questions/18822552/setting-network-timeout-for-jdbc-connection

        ret.setConnectionProperties(props);
        ret.setURL("jdbc:oracle:thin:/@FONO");
        return ret;
    }

    static DAO getDAO() throws SQLException {
        return new DAOImpl(buildOracleDataSource());
    }

    static Properties loadProperties() {
        Properties ret = null;
        try (InputStream in = DAO.class.getClassLoader().getResourceAsStream(PROPERTY_PATH)) {
            if (in == null) {
                LOGGER.log(SEVERE, "net.redvoiss.sms.util.property_file.null", new Object[]{PROPERTY_PATH});
            } else {
                ret = new Properties();
                ret.load(in);
            }
        } catch (FileNotFoundException e) {
            LOGGER.log(SEVERE, "net.redvoiss.sms.util.property_file.missing", e);
        } catch (IOException e) {
            LOGGER.log(SEVERE, "net.redvoiss.sms.util.io", e);
        }
        return ret;
    }

    enum MessageState {
        NEW("0"), PENDING("1");

        private final String state;

        MessageState(String state) {
            this.state = state;
        }

        public String getState() {
            return state;
        }
    }

    enum ReportState {
        INITIAL("0"), OK("1"), ERROR("2"), ABANDON("3");

        private final String state;

        ReportState(String state) {
            this.state = state;
        }

        public String getState() {
            return state;
        }
    }

    enum HistoricalState {
        OK("0"), ERROR("1"), ABANDON("2");

        private final String state;

        HistoricalState(String state) {
            this.state = state;
        }

        public String getState() {
            return state;
        }
    }

    static String creaDespachaLoteCompleto(Connection conn, String username, String destination, String message,
            String idgw, String batchId, boolean isLast, String description, String messageParameter,
            boolean isCommercial, Date date, String idcliente) throws SQLException {
        String ret = "";
        
        //LOGGER.log(SEVERE,String.format(
        //        "En creaDespachaLoteCompleto, Username is {%s}, destination is {%s}, message is {%s}, gw id is {%s}, batch id is {%s}, is last {%b}, batch name is {%s}, parameter 4 is {%s}, is commercial {%b}, date {%s}",
        //        username, destination, message, idgw, batchId, isLast, description, messageParameter, isCommercial,
        //        DATE_FORMATTER.format(date)));
        try (CallableStatement cs = conn
                .prepareCall("{call pkg_fc_sms.CreaDespachaLoteCompleto(?,?,?,?,?,?,?,?,?,?,?,?)}")) { //Fixs LAEL-8
            cs.setString(1, username);
            cs.setString(2, destination);
            cs.setString(3, message);
            cs.setString(4, idgw);
            cs.setString(5, batchId == null ? "" : batchId);
            cs.setString(6, batchId == null || batchId.isEmpty() ? "1" : "0");
            cs.setString(7, isLast ? "1" : "0");
            cs.registerOutParameter(8, 4);
            cs.setString(9, description);
            cs.setString(10, messageParameter);
            cs.setInt(11, isCommercial ? 1 : 0);
            cs.setString(12, DATE_FORMATTER.format(date));
            cs.execute();
            ret = cs.getString(8);//LAEL-17
        } catch (SQLException sqle) {
            if (sqle.getErrorCode() == 20001) {//Balance too low
                    LOGGER.log(WARNING, String.format("Balance too low to store message to %s", username), sqle);
            }
            else if (sqle.getErrorCode() == 20002) {//Mobile number is on Blacklist
                    LOGGER.log(WARNING, String.format("Mobile %s is on blacklist", destination), sqle);
            }
            //throw sqle;
        }
        return ret;
    }

    static String creaDespachaLoteCompletoReply(Connection conn, String username, String destination, String message,
            String idgw, String batchId, boolean isLast, String description, String messageParameter,
            boolean isCommercial, Date date, String idcliente) throws SQLException {
        String ret = "";
        LOGGER.finest(String.format(
                "Username is {%s}, destination is {%s}, message is {%s}, gw id is {%s}, batch id is {%s}, is last {%b}, batch name is {%s}, parameter 4 is {%s}, is commercial {%b}, date {%s}",
                username, destination, message, idgw, batchId, isLast, description, messageParameter, isCommercial,
                DATE_FORMATTER.format(date)));
        try (CallableStatement cs = conn
                .prepareCall("{call pkg_fc_sms.CreaDespachaLoteCompleto_Reply(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, ?)}")) { //Fixs LAEL-8
            cs.setString(1, username);
            cs.setString(2, destination);
            cs.setString(3, message);
            cs.setString(4, idgw);
            cs.setString(5, batchId == null ? "" : batchId);
            cs.setString(6, batchId == null || batchId.isEmpty() ? "1" : "0");
            cs.setString(7, isLast ? "1" : "0");
            cs.registerOutParameter(8, 4);
            cs.setString(9, description);
            cs.setString(10, messageParameter);
            cs.setInt(11, isCommercial ? 1 : 0);
            cs.setString(12, DATE_FORMATTER.format(date));
            cs.setInt(13, 1);//Reply required
            cs.setString(14, "0");//Default value
            cs.setInt(15, 3);//Type WS
            cs.setString(16, idcliente);
            cs.execute();
            ret = cs.getString(8);//LAEL-17
        } catch (SQLException sqle) {
            if (sqle.getErrorCode() == 20001) {//Balance too low
                    LOGGER.log(WARNING, String.format("Balance too low to store message to %s", username), sqle);
            }
            else if (sqle.getErrorCode() == 20002) {//Mobile number is on Blacklist
                    LOGGER.log(WARNING, String.format("Mobile %s is on blacklist", destination), sqle);
            }
            throw sqle;
        }
        return ret;
    }

    // CGF 2020-10-20. Creo Lote marcado como invalido por destino fallido

    static String creaDespachaLoteConItemFallido(Connection conn, String username, String destination, String message,
            String idgw, String batchId, boolean isLast, String description, String messageParameter,
            boolean isCommercial, Date date) throws SQLException {
        String ret;
        //LOGGER.warning(() -> String.format("EN creaDespachaLoteConItemFallido() para {%s}",destination));
        LOGGER.finest(String.format(
                "Username is {%s}, destination is {%s}, message is {%s}, gw id is {%s}, batch id is {%s}, is last {%b}, batch name is {%s}, parameter 4 is {%s}, is commercial {%b}, date {%s}",
                username, destination, message, idgw, batchId, isLast, description, messageParameter, isCommercial,
                DATE_FORMATTER.format(date)));
        try (CallableStatement cs = conn
                .prepareCall("{call pkg_fc_sms.CreaDespachaLoteFallido(?,?,?,?,?,?,?,?,?,?,?,?)}")) { //Fixs LAEL-8
            cs.setString(1, username);
            cs.setString(2, destination);
            cs.setString(3, message);
            cs.setString(4, idgw);
            cs.setString(5, batchId == null ? "" : batchId);
            cs.setString(6, batchId == null || batchId.isEmpty() ? "1" : "0");
            cs.setString(7, isLast ? "1" : "0");
            cs.registerOutParameter(8, 4);
            cs.setString(9, description);
            cs.setString(10, messageParameter);
            cs.setInt(11, isCommercial ? 1 : 0);
            cs.setString(12, DATE_FORMATTER.format(date));
            cs.execute();
            ret = cs.getString(8); 
        }
        return ret;
    }

    static String creaDespachaLoteConItemFallidoReply(Connection conn, String username, String destination, String message,
            String idgw, String batchId, boolean isLast, String description, String messageParameter,
            boolean isCommercial, Date date) throws SQLException {
        String ret;
        LOGGER.finest(String.format(
                "Username is {%s}, destination is {%s}, message is {%s}, gw id is {%s}, batch id is {%s}, is last {%b}, batch name is {%s}, parameter 4 is {%s}, is commercial {%b}, date {%s}",
                username, destination, message, idgw, batchId, isLast, description, messageParameter, isCommercial,
                DATE_FORMATTER.format(date)));
        try (CallableStatement cs = conn
                .prepareCall("{call pkg_fc_sms.CreaDespachaLoteFallido_Reply(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}")) { //Fixs LAEL-8
            cs.setString(1, username);
            cs.setString(2, destination);
            cs.setString(3, message);
            cs.setString(4, idgw);
            cs.setString(5, batchId == null ? "" : batchId);
            cs.setString(6, batchId == null || batchId.isEmpty() ? "1" : "0");
            cs.setString(7, isLast ? "1" : "0");
            cs.registerOutParameter(8, 4);
            cs.setString(9, description);
            cs.setString(10, messageParameter);
            cs.setInt(11, isCommercial ? 1 : 0);
            cs.setString(12, DATE_FORMATTER.format(date));
            cs.setInt(13, 1);//Reply required
            cs.setString(14, "0");//Default value
            cs.setInt(15, 3);//Type WS
            cs.execute();
            ret = cs.getString(8); 
        }
        return ret;
    }
    // Fin.
    /*   BACKUP CGF 20220927-1600
    
    static void creaDespachaLoteSimpleReply(Connection conn, String systemId, String destinationAddress, String sourceAddress, String message, String messageId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO FS_SMSCGW (MSGID,SENDER,SOURCEADDRESS,DSTADDRESS,MESSAGE,RECEPTION_DATE, ID_REPORT) VALUES (?,?,?,?,?,localtimestamp,?)")) {
            ps.setString(1, messageId);
            ps.setString(2, systemId);
            ps.setString(3, sourceAddress);
            ps.setString(4, destinationAddress);
            ps.setString(5, message);
            try (CallableStatement cs = conn.prepareCall("{call pkg_fc_sms.CreaDespachaLoteSimple_Reply(?,?,?,?,?,?,?,?)}")) {
                cs.setString(1, systemId);
                cs.setString(2, destinationAddress);
                cs.setString(3, message);
                cs.setString(4, messageId);
                cs.setInt(5, 0);
                cs.setInt(6, 1);
                cs.setString(7, sourceAddress);
                cs.setInt(8, 1);
                cs.executeQuery();
                ps.setString(6, DAO.ReportState.INITIAL.getState());
                ps.executeQuery();
            } catch (SQLException sqle) {
                if (sqle.getErrorCode() == 20001) {//Balance too low
                    LOGGER.log(WARNING, String.format("Balance too low to store message %s to %s", messageId, systemId), sqle);
                }
                else if (sqle.getErrorCode() == 20002) {//Mobile number is on Blacklist
                    LOGGER.log(WARNING, String.format("Mobile %s is on blacklist", destinationAddress), sqle);
                }
                throw sqle;
            }
        }
    }

    
    static void creaDespachaLSR_SMPP(Connection conn, String systemId, String destinationAddress, String sourceAddress, String message, String messageId ,
                                            short esmClass, byte[] udhHeader) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO FS_SMSCGW (MSGID,SENDER,SOURCEADDRESS,DSTADDRESS,MESSAGE,RECEPTION_DATE, ID_REPORT) VALUES (?,?,?,?,?,SYSDATE,?)")) {
            ps.setString(1, messageId);
            ps.setString(2, systemId);
            ps.setString(3, sourceAddress);
            ps.setString(4, destinationAddress);
            ps.setString(5, message);
            
            //LOGGER.log(WARNING, "En creaDespachaLSR_SMPP esmClass="+esmClass);            
            //LOGGER.log(WARNING, "  --> udhHeader="+new String(udhHeader));            
             //CreaDespachaLSR_SMPP(pusername, pdestino , ptexto , psmsidgw , pcomercial, preply , source_address , pid_sms_s_type, pid_cliente, pesm_class , pudh_header ) IS
  
            try (CallableStatement cs = conn.prepareCall("{call pkg_fc_sms.creaDespachaLSR_SMPP(?,?,?,?,?,?,?,?,?,?,?)}")) {
                cs.setString(1, systemId);
                cs.setString(2, destinationAddress);
                cs.setString(3, message);
                cs.setString(4, messageId);
                cs.setInt(5, 0);
                cs.setInt(6, 1);
                cs.setString(7, sourceAddress);
                cs.setInt(8, 1);
                cs.setInt(9, 1);
                cs.setShort(10, esmClass);
                InputStream in = new ByteArrayInputStream(udhHeader);
                cs.setBlob(11, in, udhHeader.length);                
                cs.executeQuery();

                ps.setString(6, DAO.ReportState.INITIAL.getState());
                ps.executeQuery();
            } catch (SQLException sqle) {
                if (sqle.getErrorCode() == 20001) {//Balance too low
                    LOGGER.log(WARNING, String.format("Balance too low to store message %s to %s", messageId, systemId), sqle);
                }
                else if (sqle.getErrorCode() == 20002) {//Mobile number is on Blacklist
                    LOGGER.log(WARNING, String.format("Mobile %s is on blacklist", destinationAddress), sqle);
                }
                throw sqle;
            }
        }
    }
  
    
      
    */
    /*static void creaDespachaLoteSimpleReply(Connection conn, String systemId, String destinationAddress, String sourceAddress, String message, String messageId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO FS_SMSCGW (MSGID,SENDER,SOURCEADDRESS,DSTADDRESS,MESSAGE,RECEPTION_DATE, ID_REPORT) VALUES (?,?,?,?,?,localtimestamp,?)")) {
            ps.setString(1, messageId);
            ps.setString(2, systemId);
            ps.setString(3, sourceAddress);
            ps.setString(4, destinationAddress);
            ps.setString(5, message);
            try (CallableStatement cs = conn.prepareCall("{call pkg_fc_sms.CreaDespachaLoteSimple_Reply(?,?,?,?,?,?,?,?)}")) {
                cs.setString(1, systemId);
                cs.setString(2, destinationAddress);
                cs.setString(3, message);
                cs.setString(4, messageId);
                cs.setInt(5, 0);
                cs.setInt(6, 1);
                cs.setString(7, sourceAddress);
                cs.setInt(8, 1);
                cs.executeQuery();
                ps.setString(6, DAO.ReportState.INITIAL.getState());
                ps.executeQuery();
            } catch (SQLException sqle) {
                if (sqle.getErrorCode() == 20001) {//Balance too low
                    LOGGER.log(WARNING, String.format("Balance too low to store message %s to %s", messageId, systemId), sqle);
                }
                else if (sqle.getErrorCode() == 20002) {//Mobile number is on Blacklist
                    LOGGER.log(WARNING, String.format("Mobile %s is on blacklist", destinationAddress), sqle);
                }
                throw sqle;
            }
        }
    }

    
    static void creaDespachaLSR_SMPP(Connection conn, String systemId, String destinationAddress, String sourceAddress, String message, String messageId ,
                                            short esmClass, byte[] udhHeader) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO FS_SMSCGW (MSGID,SENDER,SOURCEADDRESS,DSTADDRESS,MESSAGE,RECEPTION_DATE, ID_REPORT) VALUES (?,?,?,?,?,SYSDATE,?)")) {
            ps.setString(1, messageId);
            ps.setString(2, systemId);
            ps.setString(3, sourceAddress);
            ps.setString(4, destinationAddress);
            ps.setString(5, message);
            
            //LOGGER.log(WARNING, "En creaDespachaLSR_SMPP esmClass="+esmClass);            
            //LOGGER.log(WARNING, "  --> udhHeader="+new String(udhHeader));            
             //CreaDespachaLSR_SMPP(pusername, pdestino , ptexto , psmsidgw , pcomercial, preply , source_address , pid_sms_s_type, pid_cliente, pesm_class , pudh_header ) IS
  
            try (CallableStatement cs = conn.prepareCall("{call pkg_fc_sms.creaDespachaLSR_SMPP(?,?,?,?,?,?,?,?,?,?,?)}")) {
                cs.setString(1, systemId);
                cs.setString(2, destinationAddress);
                cs.setString(3, message);
                cs.setString(4, messageId);
                cs.setInt(5, 0);
                cs.setInt(6, 1);
                cs.setString(7, sourceAddress);
                cs.setInt(8, 1);
                cs.setInt(9, 1);
                cs.setShort(10, esmClass);
                InputStream in = new ByteArrayInputStream(udhHeader);
                cs.setBlob(11, in, udhHeader.length);                
                cs.executeQuery();

                ps.setString(6, DAO.ReportState.INITIAL.getState());
                ps.executeQuery();
            } catch (SQLException sqle) {
                if (sqle.getErrorCode() == 20001) {//Balance too low
                    LOGGER.log(WARNING, String.format("Balance too low to store message %s to %s", messageId, systemId), sqle);
                }
                else if (sqle.getErrorCode() == 20002) {//Mobile number is on Blacklist
                    LOGGER.log(WARNING, String.format("Mobile %s is on blacklist", destinationAddress), sqle);
                }
                throw sqle;
            }
        }
    }*/
  
    static String getDestino(Connection conn, String destinationAddress) throws SQLException {
    String ret = "";
            LOGGER.log(WARNING, "CGF: EN getDestino con destinationAddress  '"+destinationAddress+"' ");
            String     sSQL = "{call pkg_fc_sms.GetDestino(?)}";        
            try (CallableStatement cs = conn.prepareCall(sSQL)) {                
                cs.setString(1, destinationAddress);
                cs.executeQuery();
                ret = cs.getString(1);
                
            } catch (SQLException sqle) {
                if (sqle.getErrorCode() == 20001) { 
                    LOGGER.log(WARNING, String.format("Exception calling getDestino para %s", destinationAddress), sqle);
                }                
                throw sqle;
            }
        return ret;
    }    
    
    static void creaDespachaLoteSimpleReply(Connection conn, String systemId, String destinationAddress, String sourceAddress, String message, String messageId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO FS_SMSCGW (MSGID,SENDER,SOURCEADDRESS,DSTADDRESS,MESSAGE,RECEPTION_DATE, ID_REPORT) VALUES (?,?,?,?,?,SYSDATE,?)")) {
            ps.setString(1, messageId);
            ps.setString(2, systemId);
            ps.setString(3, sourceAddress);
            ps.setString(4, destinationAddress);
            ps.setString(5, message);
            
            int esComercial = 1;
            //LOGGER.log(WARNING, "CGF: EN creaDespachaLoteSimpleReply con systemID="+systemId+", desde '"+sourceAddress+"' a '"+destinationAddress+"' ");
            String     sSQL = "{call pkg_fc_sms.CreaDespachaLoteSimple_Reply(?,?,?,?,?,?,?,?)}";        
            if ("DV_56442212300".equals(systemId) || "DV_56442212301".equals(systemId))
            {
                if ("56".equals(sourceAddress.substring(0,2)))
                {
                    sSQL = "{call pkg_fc_sms.CreaDespachaLote_Reply_DIGEVO(?,?,?,?,?,?,?,?)}";
                    LOGGER.log(WARNING, "CGF: Cambio SP en creaDespachaLoteSimpleReply: "+sSQL);    
                } 
                if ("56442212310".equals(sourceAddress))
                    esComercial = 0;           
            }
            
            //LOGGER.log(WARNING, "En creaDespachaLoteSimpleReply messageId="+messageId+", message:"+ message);                
            try (CallableStatement cs = conn.prepareCall(sSQL)) {                
            //try (CallableStatement cs = conn.prepareCall("{call pkg_fc_sms.CreaDespachaLoteSimple_Reply(?,?,?,?,?,?,?,?)}")) {
                cs.setString(1, systemId);
                cs.setString(2, destinationAddress);
                cs.setString(3, message); 
                cs.setString(4, messageId);
                cs.setInt(5, esComercial);  // por defecto COMERCIAL , cambiar a 0 para no comercial
                cs.setInt(6, 1);
                cs.setString(7, sourceAddress);
                cs.setInt(8, 1);
                cs.executeQuery();

                ps.setString(6, DAO.ReportState.INITIAL.getState());
                ps.executeQuery();
                
            } catch (SQLException sqle) {
                if (sqle.getErrorCode() == 20001) {//Balance too low
                    LOGGER.log(WARNING, String.format("Balance too low to store message %s", messageId), sqle);
                    
                } else if (sqle.getErrorCode() == 20002) {//Mobile number is on Blacklist
                    LOGGER.log(WARNING, String.format("Mobile %s is on blacklist", destinationAddress), sqle);
                }                
                throw sqle;
            }
        }
    }
    
    static void creaDespachaLSR_SMPP(Connection conn, String systemId, String destinationAddress, String sourceAddress, String message, String messageId ,
                                            short esmClass, byte[] udhHeader) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO FS_SMSCGW (MSGID,SENDER,SOURCEADDRESS,DSTADDRESS,MESSAGE,RECEPTION_DATE, ID_REPORT) VALUES (?,?,?,?,?,SYSDATE,?)")) {
            ps.setString(1, messageId);
            ps.setString(2, systemId);
            ps.setString(3, sourceAddress);
            ps.setString(4, destinationAddress);
            ps.setString(5, message);
   
            int esComercial = 1;
            LOGGER.log(WARNING, "CGF  --> EN creaDespachaLSR_SMPP con systemID="+systemId+", desde '"+sourceAddress+"' a '"+destinationAddress+"' ");
            String     sSQL = "{call pkg_fc_sms.creaDespachaLSR_SMPP(?,?,?,?,?,?,?,?,?,?,?)}";   
            
            if ("DV_56442212300".equals(systemId) || "DV_56442212301".equals(systemId))
            {
                if ("56".equals(sourceAddress.substring(0,2)))
                {
                    sSQL = "{call pkg_fc_sms.creaDespachaLSR_SMPP_DIGEVO(?,?,?,?,?,?,?,?,?,?,?)}";
                    LOGGER.log(WARNING, "--> Cambio SP en creaDespachaLSR_SMPP: "+sSQL);            
                }  
                if ("56442212310".equals(sourceAddress))
                    esComercial = 0;           
            }

            try (CallableStatement cs = conn.prepareCall(sSQL)) {     //Fixs LAEL-8
               
            //try (CallableStatement cs = conn.prepareCall("{call pkg_fc_sms.creaDespachaLSR_SMPP(?,?,?,?,?,?,?,?,?,?,?)}")) {
                cs.setString(1, systemId);
                cs.setString(2, destinationAddress);
                cs.setString(3, message);
                cs.setString(4, messageId);
                cs.setInt(5, esComercial);  // por defecto COMERCIAL , cambiar a 0 para no comercial
                cs.setInt(6, 1);
                cs.setString(7, sourceAddress);
                cs.setInt(8, 1);
                cs.setInt(9, 1);
                cs.setShort(10, esmClass);
                InputStream in = new ByteArrayInputStream(udhHeader);
                cs.setBlob(11, in, udhHeader.length);               

                //LOGGER.warning(String.format("EJECUTARE: call pkg_fc_sms.creaDespachaLSR_SMPP(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)",
                //    systemId,destinationAddress,message,messageId,"0","1",sourceAddress,"1","1",""+esmClass,""+udhHeader.toString()));
                
                cs.executeQuery();

                ps.setString(6, DAO.ReportState.INITIAL.getState());
                ps.executeQuery();
            } catch (SQLException sqle) {
                if (sqle.getErrorCode() == 20001) {//Balance too low
                    LOGGER.log(WARNING, String.format("Balance too low to store message %s", messageId), sqle);
                } else if (sqle.getErrorCode() == 20002) {//Mobile number is on Blacklist
                    LOGGER.log(WARNING, String.format("Mobile %s is on blacklist", destinationAddress), sqle);
                }
                throw sqle;
            }
        }
    }
   
    
    static List<NewMessage> getSMSList(Connection c, int route, int quantity) throws SQLException {
        List<NewMessage> ret = new ArrayList<>();
        final String propertyName = "net.redvoiss.sms.dao.check.sms";
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                LOGGER.log(FINE, "net.redvoiss.sms.dao.check.sms.parameters", new Object[]{route, quantity});
                s.setString(1, MessageState.NEW.getState());
                s.setInt(2, route);
                s.setInt(3, route);
                s.setInt(4, quantity + 1);
                try (ResultSet rs = s.executeQuery()) {
                    while (rs.next()) {
                        final String smsCode = rs.getString("SMS_COD");
                        final String sourceNumber = rs.getString("SOURC_ADDRS");
                        final String destinationNumber = rs.getString("DST_ADDRS");
                        final String message = rs.getString("SMS_TXT");
                        final String msgIdGW = rs.getString("MSGID_GW");

                        LOGGER.log(WARNING, "SMS a numero corto: DST_ADDRS: "+ destinationNumber+", SOURC_ADDRS: "+sourceNumber+", sourceNumber: "+sourceNumber);
           
                        assert route == rs.getInt("CODE_RUTA");
                        NewMessage mtbp = new NewMessage(smsCode, msgIdGW, sourceNumber, destinationNumber, message, route);
                        ret.add(mtbp);
                        LOGGER.log(FINE, "net.redvoiss.sms.dao.check.sms.result", new Object[]{mtbp});
                    }
                }
            }
        }
        return ret;
    }

    static int enqueued(Connection c, String gsmCode, String smsCode) throws SQLException {
        int ret = 0;
        final String propertyName = "net.redvoiss.sms.dao.enqueued";
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                LOGGER.log(FINE, propertyName, new Object[]{smsCode, gsmCode});
                s.setString(1, gsmCode);
                s.setString(2, smsCode);
                ret = s.executeUpdate();
            }
        }
        return ret;
    }

    static int sent(Connection c, String gsmCode, String smsCode, Date sentDate) throws SQLException {
        int ret = 0;
        final String propertyName = "net.redvoiss.sms.dao.message_ok";
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                LOGGER.log(FINE, propertyName, new Object[]{smsCode, gsmCode});
                s.setString(1, MessageState.PENDING.getState());
                s.setString(2, gsmCode);
                s.setTimestamp(3, new Timestamp(sentDate.getTime()));
                s.setString(4, smsCode);
                ret = s.executeUpdate();
            }
        }
        return ret;
    }

    static int transmitted(Connection c, String smsCode) throws SQLException {
        int ret = 0;
        final String propertyName = "net.redvoiss.sms.dao.message_transmitted";
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                LOGGER.log(FINE, propertyName, new Object[]{smsCode});
                s.setString(1, MessageState.PENDING.getState());
                s.setString(2, smsCode);
                ret = s.executeUpdate();
            }
        }
        return ret;
    }

    static int sent(Connection c, String messageId, String smsCode) throws SQLException {
        int ret = 0;
        final String propertyName = "net.redvoiss.sms.dao.message_ok_sysdate";
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                LOGGER.log(FINE, propertyName, new Object[]{smsCode, messageId});
                s.setString(1, messageId);
                s.setString(2, smsCode);
                ret = s.executeUpdate();
            }
        }
        return ret;
    }

    static int register_sent2(Connection c, String messageId, String smsCode) throws SQLException {
        int ret = 0;
        final String propertyName = "net.redvoiss.sms.dao.message_ok_status"; // UPDATE FS_ESME SET STATUS=?, CODE_SMSC=?, FECHA_HORA=sysdate WHERE SMS_COD=?
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            //LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            String sql = "UPDATE FS_ESME SET STATUS="+MessageState.PENDING.getState()+", CODE_SMSC='"+messageId+"', FECHA_HORA=sysdate WHERE SMS_COD="+smsCode;
                LOGGER.log(SEVERE,"SQL: "+sql);
            //try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
            try (PreparedStatement s = c.prepareStatement(sql)) {
                LOGGER.log(FINE, propertyName, new Object[]{smsCode, messageId});
                //s.setString(1, MessageState.PENDING.getState());
                //s.setString(2, messageId);
                //s.setString(3, smsCode);
                //LOGGER.log(SEVERE, "net.redvoiss.sms.dao.statement:", (new Object[]{sqlStatement}).toString());
                ret = s.executeUpdate();
            }
        }
        return ret;
    }

    static boolean cleanFaultyRecord(Connection c, String smsCode, String idGW) throws SQLException {
        boolean ret = false;
        try {
            c.setAutoCommit(false);
            ret = moveFaulty(c, smsCode) && (deleteSMS(c, smsCode) > 0);
            if (idGW == null) {
                LOGGER.log(FINE, "net.redvoiss.sms.dao.smsgw.propagation_unnecessary", smsCode);
            } else {
                ret &= actualizaSMSGW(c, idGW, ReportState.ERROR);
            }
            c.commit();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException re) {
                LOGGER.log(SEVERE, "net.redvoiss.sms.dao.rollback.failed", re);
            }
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
        return ret;
    }

    static boolean storeSuccessRecord(Connection c, String smsCode, String idGW, int channel, Date confirmationDate) throws SQLException {
        boolean ret = false;
        try {
            c.setAutoCommit(false);
            ret = updateSuccessChannelStatistics(smsCode, channel, c);
            ret &= moveOK(c, smsCode, confirmationDate);
            ret &= deleteSMS(c, smsCode) > 0;
            if (idGW == null) {
                LOGGER.log(FINE, "net.redvoiss.sms.dao.smsgw.propagation_unnecessary", smsCode);
            } else {
                ret &= actualizaSMSGW(c, idGW, ReportState.OK);
            }
            c.commit();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException re) {
                LOGGER.log(SEVERE, "net.redvoiss.sms.dao.rollback.failed", re);
            }
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
        return ret;
    }

    static boolean storeSuccessRecord(Connection c, String smsCode, int channel, Date confirmationDate) throws SQLException {
        boolean ret = false;
        try {
            c.setAutoCommit(false);
            ret = updateSuccessChannelStatistics(smsCode, channel, c);
            ret &= moveOK(c, smsCode, confirmationDate);
            ret &= deleteSMS(c, smsCode) > 0;

            c.commit();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException re) {
                LOGGER.log(SEVERE, "net.redvoiss.sms.dao.rollback.failed", re);
            }
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
        return ret;
    }    
    
    static boolean updateSuccessChannelStatistics(String smsCode, int channel, Connection c)
            throws SQLException {
        boolean ret = false;
        final String propertyName = "net.redvoiss.sms.dao.success_statistics";
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                LOGGER.log(FINE, propertyName, new Object[]{channel, smsCode});
                s.setString(1, smsCode);
                s.setInt(2, channel);
                int rowsAffected = s.executeUpdate();
                ret = rowsAffected > 0;
            }
        }
        return ret;
    }

    static boolean moveFaulty(Connection c, String smsCode) throws SQLException {
        return move(c, smsCode, new Date(), HistoricalState.ERROR) > 0;
    }

    static boolean moveOK(Connection c, String smsCode, Date confirmationDate) throws SQLException {
        return DAO.move(c, smsCode, confirmationDate, HistoricalState.OK) > 0;
    }

    static boolean moveAbandoned(Connection c, String smsCode, Date confirmationDate) throws SQLException {
        return DAO.move(c, smsCode, confirmationDate, HistoricalState.ABANDON) > 0;
    }

    static int deleteSMS(Connection c, String smsCode) throws SQLException {
        int ret = 0;
        final String propertyName = "net.redvoiss.sms.dao.delete_entry";
        LOGGER.log(FINE, propertyName, new Object[]{smsCode});
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                LOGGER.log(FINE, "net.redvoiss.sms.dao.delete_entry.parameters", new Object[]{smsCode});
                s.setString(1, smsCode);
                ret = s.executeUpdate();
            }
        }
        return ret;
    }

    static int move(Connection c, String smsCode, Date confirmationDate, HistoricalState state)
            throws SQLException {
        int ret = 0;
        final String propertyName = "net.redvoiss.sms.dao.move";
        LOGGER.log(FINE, propertyName, new Object[]{smsCode});
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                LOGGER.log(FINE, propertyName, new Object[]{smsCode});
                s.setString(1, state.getState());
                s.setTimestamp(2, new Timestamp(confirmationDate.getTime()));
                s.setString(3, smsCode);
                ret = s.executeUpdate();
            }
        }
        return ret;
    }

    static int storeSequenceNumber(Connection c, String smsCode, int iterator) throws SQLException {
        int ret = 0;
        final String propertyName = "net.redvoiss.sms.dao.update_iterator";
        LOGGER.log(FINE, propertyName, new Object[]{smsCode, iterator});
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                LOGGER.log(FINE, propertyName, new Object[]{smsCode});
                s.setInt(1, iterator);
                s.setString(2, DAO.MessageState.PENDING.getState());
                s.setString(3, smsCode);
                ret = s.executeUpdate();
            }
        }
        return ret;
    }

    static List<Message> findMessage(Connection c, String messageId, int sequenceNumber) throws SQLException {
        List<Message> ret = new ArrayList<>();
        final String propertyName = "net.redvoiss.sms.dao.find_entry";
        LOGGER.log(FINE, propertyName, new Object[]{messageId, sequenceNumber});
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                LOGGER.log(FINE, propertyName, new Object[]{messageId});
                s.setString(1, messageId);
                s.setInt(2, sequenceNumber);
                try (ResultSet rs = s.executeQuery()) {
                    while (rs.next()) {
                        final String smsCode = rs.getString(1);
                        final String smscGWId = rs.getString(2);
                        final int route = rs.getInt(3);
                        ret.add(new Message(smsCode, smscGWId, route));
                    }
                }
            }
        }
        return ret;
    }

    static List<Message> findMessageWithoutSequence(Connection c, String messageId) throws SQLException {
        List<Message> ret = new ArrayList<>();
        final String propertyName = "net.redvoiss.sms.dao.find_entry_without_sequence";
        LOGGER.log(FINE, propertyName, new Object[]{messageId});
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            LOGGER.log(FINE, "net.redvoiss.sms.dao.statement", new Object[]{sqlStatement});
            try (PreparedStatement s = c.prepareStatement(sqlStatement)) {
                LOGGER.log(FINE, propertyName, new Object[]{messageId});
                s.setString(1, messageId);
                try (ResultSet rs = s.executeQuery()) {
                    while (rs.next()) {
                        final String smsCode = rs.getString(1);
                        final String smscGWId = rs.getString(2);
                        final int route = rs.getInt(3);
                        ret.add(new Message(smsCode, smscGWId, route));
                    }
                }
            }
        }
        return ret;
    }

    static boolean actualizaSMSGW(Connection conn, String gwId, ReportState reportState) throws SQLException {
        boolean ret = false;
        final String propertyName = "net.redvoiss.sms.dao.smsgw";
        final String sqlStatement = SQL_PROPERTIES.getProperty(propertyName);
        if (sqlStatement == null) {
            LOGGER.log(WARNING, "net.redvoiss.sms.dao.property_name.missing", new Object[]{propertyName});
        } else {
            try (PreparedStatement s = conn.prepareStatement(sqlStatement)) {
                LOGGER.log(FINE, propertyName, new Object[]{gwId, reportState});
                s.setString(1, reportState.getState());
                Timestamp ts = new Timestamp(new Date().getTime());
                s.setTimestamp(2, ts);
                s.setString(3, gwId);
                int rowsAffected = s.executeUpdate();
                if (!(ret = rowsAffected > 0)) {
                    LOGGER.log(SEVERE, "net.redvoiss.sms.dao.smsgw_update.failed", new Object[]{gwId, reportState});
                }
            }
        }
        return ret;
    }

    static boolean storeAbandonedRecord(Connection conn, String smsCode, String idGW, int channel, Date confirmationDate) throws SQLException {
        boolean ret = false;
        try {
            conn.setAutoCommit(false);
            ret = updateSuccessChannelStatistics(smsCode, channel, conn);
            /**
             * BEGIN NOTE Originally entry was not stored when a message is
             * received using SMPP Now it seems to be consistent to store this
             * in order to reflect the state in DB.
             */
            ret &= moveAbandoned(conn, smsCode, confirmationDate);
            /* END NOTE */
            if (idGW == null) {
                LOGGER.log(FINE, "net.redvoiss.sms.dao.smsgw.propagation_unnecessary", smsCode);
            } else {
                ret &= actualizaSMSGW(conn, idGW, ReportState.ABANDON);
            }
            ret &= (deleteSMS(conn, smsCode) > 0);
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException re) {
                LOGGER.log(SEVERE, "net.redvoiss.sms.dao.rollback.failed", re);
            }
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return ret;
    }

    static boolean storeFaultyRecord(Connection conn, String smsCode, String idGW, int channel) throws SQLException {
        boolean ret = false;
        try {
            conn.setAutoCommit(false);
            ret = moveFaulty(conn, smsCode); //See note on {@link #storeSuccessRecord( String , int , String , Date) storeSuccessRecord}
            if (idGW == null) {
                LOGGER.log(FINE, "net.redvoiss.sms.dao.smsgw.propagation_unnecessary", smsCode);
            } else {
                ret &= actualizaSMSGW(conn, idGW, ReportState.ERROR);
            }
            ret &= (deleteSMS(conn, smsCode) > 0);
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException re) {
                LOGGER.log(SEVERE, "net.redvoiss.sms.dao.rollback.failed", re);
            }
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return ret;
    }
}
