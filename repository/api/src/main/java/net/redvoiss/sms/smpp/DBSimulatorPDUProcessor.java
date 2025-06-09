package net.redvoiss.sms.smpp;

import org.smpp.Data;
import org.smpp.pdu.BindRequest;
import org.smpp.pdu.DeliverSM;
import org.smpp.pdu.PDUException;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.Unbind;
import org.smpp.pdu.WrongLengthOfStringException;
import org.smpp.smscsim.SMSCSession;
import org.smpp.smscsim.ShortMessageStore;
import org.smpp.smscsim.SimulatorPDUProcessor;
import org.smpp.smscsim.util.Table;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.sql.DataSource;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static net.redvoiss.sms.dao.DAO.ReportState;
import static org.smpp.smscsim.DeliveryInfoSender.DELIVERED;
import static org.smpp.smscsim.DeliveryInfoSender.EXPIRED;
import static org.smpp.smscsim.DeliveryInfoSender.UNDELIVERABLE;
import static org.smpp.smscsim.DeliveryInfoSender.UNKNOWN;

public class DBSimulatorPDUProcessor extends SimulatorPDUProcessor implements Runnable {

    protected static final Logger LOGGER = Logger.getLogger(DBSimulatorPDUProcessor.class.getName(), DBSimulatorPDUProcessor.class.getName());
    private final DataSource ds;
    private final ScheduledExecutorService scheduler;
    private Future<?> scheduledMOTask;

    public DBSimulatorPDUProcessor(DataSource ds, ScheduledExecutorService scheduler, SMSCSession session, ShortMessageStore messageStore, Table users) {
        super(session, messageStore, users);
        this.ds = ds;
        this.scheduler = scheduler;
    }

    @Override
    public void clientRequest(org.smpp.pdu.Request request) {

        if (Data.SUBMIT_SM == request.getCommandId()) {
            SubmitSM submitSM = (SubmitSM) request;
            
            /*LOGGER.log(WARNING, " ---> SUBMIT_SM con getEsmClass():"+ submitSM.getEsmClass());           
            if ((Data.SM_UDH_GSM & submitSM.getEsmClass()) == Data.SM_UDH_GSM) {
                LOGGER.log(WARNING, " Es un submitSM con particionamiento: "+ new Object[]{submitSM.debugString()});
            }*/
            if (request.canResponse()) { // will deliver a receipt by default
                submitSM.setRegisteredDelivery(Data.SM_SMSC_RECEIPT_REQUESTED);
            }
        } //else
            //LOGGER.log(WARNING, " ELSE request.getCommandId():"+ request.getCommandId()); 
        super.clientRequest(request);
    }

    @Override
    public void clientResponse(org.smpp.pdu.Response response) {
                     
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement ps1 = conn.prepareStatement("UPDATE FS_SMS_REGISTER_W t SET t.notify=2 WHERE t.notify=1 AND t.iterator=? AND t.SENDER= (select u.usua_cod from fc_usuario u where u.username = ?)")) {
                ps1.setString(1, String.valueOf(response.getSequenceNumber()));
                ps1.setString(2, getSystemId());
                
                if (ps1.executeUpdate() == 0) {       
/*
                    if (Data.DELIVER_SM_RESP == response.getCommandId()) {
                        
                        String sql = "UPDATE FS_SMSCGW set DLR_RESP=localtimestamp WHERE SEQ_NUMBER= "+response.getSequenceNumber()+" AND SENDER='"+getSystemId()+"' and (DLR_RESP is NULL) ";
                        LOGGER.log(WARNING, " ---> Un DELIVER_SM_RESP :"+ response.getSequenceNumber() +" con sql="+sql);

                        sql = "UPDATE FS_SMSCGW set DLR_RESP=localtimestamp WHERE SEQ_NUMBER= ? AND SENDER=? and (DLR_RESP is NULL) ";
                        try (PreparedStatement ps3 = conn.prepareStatement(sql)) {
                            ps3.setString(1, String.valueOf(response.getSequenceNumber()));
                            ps3.setString(2, getSystemId());
                            ps3.execute();
                        } catch (SQLException sqle) {
                              LOGGER.log(SEVERE, "DB exception (1):", sqle);
                        }
                    }        
*/
                    //try (PreparedStatement ps2 = conn.prepareStatement("DELETE FROM FS_SMSCGWNOTIFY WHERE ITERATOR=? AND SENDER=?")) {
                    //    ps2.setString(1, String.valueOf(response.getSequenceNumber()));
                    //    ps2.setString(2, getSystemId());
                    //    ps2.execute();
                    //}
                }
            }
        } catch (SQLException sqle) {
            LOGGER.log(SEVERE, "DB exception", sqle);
        }
        super.clientResponse(response);
    }

    @Override
    public void exit() {
        if (isActive()) {
            if (scheduledMOTask == null) {
                LOGGER.log(WARNING, "MO verification missing for {0}", getSystemId());
            } else {
                boolean cancelledOK = scheduledMOTask.cancel(true);
                if (cancelledOK) {
                    LOGGER.log(INFO, "MO verification cancelled for {0}", getSystemId());
                    scheduledMOTask = null;
                } else {
                    LOGGER.log(WARNING, "Unable to cancel MO verification for {0}", getSystemId());
                }
            }
        } else {
            LOGGER.log(WARNING, "Unable to cancel scheduled task due to processor is innactive for {0}", getSystemId());
        }
        super.exit();
    }

    @Override
    public void stop() {
        final SMSCSession session = getSession();
        if (session == null) {
            LOGGER.log(SEVERE, "Session is missing for {0}", getSystemId());
        } else {
            final org.smpp.Connection connection = session.getConnection();
            if (connection == null) {
                LOGGER.log(SEVERE, "Connection is missing for {0}", getSystemId());
            } else if (connection.isOpened()) {   //Checks if calling SMSCSession.stop is really necessary
                try {
                    LOGGER.log(SEVERE, "In DBSimulatorPDUProcessor.stop() create Unbind server request for {0}", getSystemId());
                    serverRequest(new Unbind());   //Let's try to be kind and let peer know session is being disposed
                } catch (IOException | PDUException ex) {
                    LOGGER.log(SEVERE, "Unable to unbind.", ex);
                } finally {
                    LOGGER.log(INFO, "About to shutdown session for {0}", getSystemId());
                    getSession().stop();
                }
            }
        }
    }

    @Override
    public void run() {
        final String systemId = getSystemId(); //This is only set when bound
        if (systemId == null || !getBound()) {
            DBSimulatorPDUProcessorFactory.dumpSource(getSession(), "Session is not yet bound");
        } else if (isActive()) {
            LOGGER.log(WARNING, "CGF0 About to check if MO is available for {0}", systemId);
            try (Connection conn = ds.getConnection()) {
                try (PreparedStatement ps1 = conn.prepareStatement("SELECT t.* FROM FS_SMS_REGISTER_W t WHERE t.notify=0 AND t.sender= (SELECT u.usua_cod FROM fc_usuario u WHERE u.username = ?)")) {
                    ps1.setString(1, getSystemId());
                    try (ResultSet rs = ps1.executeQuery()) {
                        if (rs.next()) {
                            final String message = rs.getString("MESSAGE");
                            final String source = rs.getString("SOURCEADDRESS");
                            final String destination = rs.getString("DSTADDRESS");
                            String iterator = rs.getString("ITERATOR");
                            final String msgID = rs.getString("MSGID");
                            LOGGER.log(WARNING, "MO found. Original iterator is {0}, msgid id {1}", new Object[]{iterator, msgID});
                            DeliverSM submit = new DeliverSM();//Defines to send a reply as a DLR(!?)
                            try {
                                submit.setShortMessage(message);
                                submit.setDestAddr(destination);
                                submit.setSourceAddr(source);
                                //LOGGER.log(INFO, "Submito DELIVER ");
                                serverRequest(submit);
                                try (PreparedStatement ps2 = conn.prepareStatement("UPDATE FS_SMS_REGISTER_W t SET t.notify=1, t.iterator=? WHERE t.MSGID = ?")) {
                                    iterator = String.valueOf(submit.getSequenceNumber());
                                    //LOGGER.log(INFO, "MO found. New iterator is {0}, msgid id {1}", new Object[]{iterator, msgID});
                                    ps2.setString(1, iterator);
                                    ps2.setString(2, msgID);
                                    int q = ps2.executeUpdate();
                                    assert q == 1;
                                }
                            } catch (PDUException pdue) {
                                LOGGER.log(WARNING, "Exception while preparing message", pdue);
                            }
                        }
                    }
                }
            } catch (SQLException sqle) {
                LOGGER.log(SEVERE, "Unexpected DB exception", sqle);
            } catch (IOException ioe) {
                LOGGER.log(WARNING, String.format("Unexpected IO exception while sending mo for {%s}",
                        getSystemId()), ioe);
                stop();
            }
        } else {
            LOGGER.log(WARNING, "Session is innactive for {0}", getSystemId());
        }
    }

     // CGF 2021-12-21. Esta rutina pone en la cola submitRequest (via llamada a submit() los DLR que hay en la BD
    @Override
    protected final int checkIdentity(BindRequest request) {
        int ret = super.checkIdentity(request);
        if (ret == Data.ESME_ROK) {
            LOGGER.log(INFO, "Loading pending reports (DLR) for {0}", request.getSystemId());
            try (Connection conn = ds.getConnection()) {
                final String systemId = request.getSystemId();
                try (PreparedStatement ps = conn.prepareStatement("select t.msgid, t.sourceaddress, t.dstaddress, t.message, t.id_report from FS_SMSCGW t where t.msgid in ( select r.msgid from FS_SMSCGW r where r.sender = ? minus select s.msgid from FS_SMSCGWNOTIFY s where s.sender = ?) AND RECEPTION_DATE > SYSDATE - 1 order by  t.reception_date desc")) { //do not look for entries older than one day
                    ps.setString(1, systemId);
                    ps.setString(2, systemId);
                //try (PreparedStatement ps = conn.prepareStatement("select t.msgid, t.sourceaddress, t.dstaddress, t.message, t.id_report from FS_SMSCGW t where t.sender = ?  AND t.RECEPTION_DATE > (SYSDATE - 1) AND (DLR_RESP is NULL) order by t.reception_date desc")) { //do not look for entries older than one day
                //    ps.setString(1, systemId);
                    try (ResultSet rs = ps.executeQuery()) {  
                        /*if (rs.last()) {
                            int rows = rs.getRow();
                            rs.beforeFirst();
                            LOGGER.log(INFO, "Found {0} pending DLRs for {1} ", new String[]{""+rows , request.getSystemId()} );
                        }*/
                        while (rs.next()) {
                            final String msgid = rs.getString("MSGID");
                            
                            // CGF 2022-03-15. Esto lo agrego para que los DLR atrasados sean con estado correcto y no solo UNKNOWN como antes
                            final String status = rs.getString("ID_REPORT");
                            
                            int estado = UNKNOWN;                          
                            if (ReportState.INITIAL.getState().equals(status)) {
                                estado = UNKNOWN;
                            } else if (ReportState.ERROR.getState().equals(status)) {
                                estado = UNDELIVERABLE;                                            
                            } else if (ReportState.OK.getState().equals(status)) {
                                estado = DELIVERED;                                            
                            } else if (ReportState.ABANDON.getState().equals(status)) {
                                estado = EXPIRED;                                            
                            }                            
                            LOGGER.log(FINE, "Loading pending report for message {0} for {1} processor", new String[]{msgid, request.getSystemId()});
                            final SubmitSM submitSM = new SubmitSM();
                            try {
                                submitSM.setSourceAddr(rs.getString("SOURCEADDRESS"));
                                submitSM.setDestAddr(rs.getString("DSTADDRESS"));
                                submitSM.setShortMessage(rs.getString("MESSAGE"));
                                
                                //LOGGER.log(WARNING, "---> En checkIdentity() Submito "+msgid+" para "+rs.getString("DSTADDRESS"));
                                getDeliveryInfoSender().submit(this, submitSM, msgid, estado, 0); //Synthetic
                                //getDeliveryInfoSender().submit(this, submitSM, msgid, org.smpp.smscsim.DeliveryInfoSender.UNKNOWN, 0); //Synthetic
                                
                            } catch (WrongLengthOfStringException ex) {
                                LOGGER.log(WARNING, "Unexpected exception while processing message " + msgid, ex);
                            } catch (UnsupportedEncodingException ex) {
                                LOGGER.log(WARNING, "Unexpected encoding exception while processing message " + msgid, ex);
                            }
                        }
                    } catch (SQLException sqle) {
                        LOGGER.log(SEVERE, String.format("Unexpected DB exception while loading pending DLR for %s", systemId), sqle);
                    }
                } catch (SQLException sqle) {
                    LOGGER.log(SEVERE, String.format("Unexpected DB exception while getting pending DLR for %s", systemId), sqle);
                }
            } catch (SQLException sqle) {
                LOGGER.log(SEVERE, "Unexpected DB exception", sqle);
            }
            scheduledMOTask = scheduler.scheduleAtFixedRate(this, 1, 2, MINUTES);
        }
        return ret;
    }
}
