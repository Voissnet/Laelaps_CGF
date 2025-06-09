package net.redvoiss.sms.smpp;

import org.smpp.pdu.Address;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.WrongLengthOfStringException;
import org.smpp.smscsim.PDUProcessor;
import org.smpp.smscsim.DeliveryInfoSender;
import org.smpp.smscsim.SimulatorPDUProcessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

import javax.sql.DataSource;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static net.redvoiss.sms.dao.DAO.ReportState;

public class DBDeliveryInfoSender extends DeliveryInfoSender {

    private static final Logger LOGGER = Logger.getLogger(DBDeliveryInfoSender.class.getName());
    private static final String IDOIDD = System.getProperty("idoidd", "310");
    private static  String DLR_TIME = System.getProperty("dlr_milisecs", "2000");
    private static final Pattern PATTERN = Pattern.compile("^(\\d{3})" + IDOIDD + "(\\d{9})$");
    private static final long LOWER_DELAY = TimeUnit.SECONDS.toNanos(3);
    private static final long UPPER_DELAY = TimeUnit.SECONDS.toNanos(7);
    private long lastMomentDBWasHit = 0;
    private final DelayQueue<DBDeliveryInfoEntry> delayQueue = new DelayQueue<>();
    private final DataSource m_ds;

    public DBDeliveryInfoSender(DataSource ds) throws SQLException {
        m_ds = ds;
    }

    protected static String processDestination(String destination) {
        String ret = null;
        Matcher m = PATTERN.matcher(destination);
        if (m.matches()) {
            if (m.groupCount() > 1) {
                ret = IDOIDD + m.group(1) + m.group(2);
            } else {
                LOGGER.log(WARNING, "New source address is different than expected: {0}", destination);
            }
        } else {
            LOGGER.log(WARNING, "New source address is quite different than expected: {0}", destination);
        }
        return ret;
    }

    @Override
    public void submit(PDUProcessor processor, SubmitSM submitRequest, String messageId, int stat, int err) {
        DBDeliveryInfoEntry entry = new DBDeliveryInfoEntry(processor, submitRequest, stat, err, messageId);
        final String systemId = ((SimulatorPDUProcessor) processor).getSystemId();
        LOGGER.log(FINER, "Queuing tracking object identified by {0} associated to {1} processor", new String[]{messageId, systemId});
        delayQueue.add(entry);
    }

    @Override
    public void process() {
        try {
            final DBDeliveryInfoEntry deliveryInfoEntry = delayQueue.take();
            final SimulatorPDUProcessor processor = (SimulatorPDUProcessor) deliveryInfoEntry.processor;
            final String systemId = processor.getSystemId();
            final String messageId = deliveryInfoEntry.messageId;
            
            DLR_TIME = System.getProperty("dlr_milisecs", "2000");

            //LOGGER.warning( "Tracking status for message {0} by {1} processor", new Object[]{messageId, systemId});
            
            if (processor.isActive()) {
                switch (systemId) {
                    case "MOVISTAR":
                        final SubmitSM submit = deliveryInfoEntry.submit;
                        if (submit == null) {
                            LOGGER.warning("Message is missing");
                        } else {
                            final Address sourceAddress = submit.getSourceAddr();
                            if (sourceAddress == null) {
                                LOGGER.warning("Source address is missing");
                            }
                            final Address destinationAddress = submit.getDestAddr();
                            if (destinationAddress == null) {
                                LOGGER.warning("New source address is missing");
                            } else {
                                try {
                                    final String destination = processDestination(destinationAddress.getAddress());
                                    if (destination == null) {
                                        LOGGER.warning("Unable to process destination");
                                    } else {
                                        destinationAddress.setAddress(destination);
                                        LOGGER.warning( "Tracking status for message "+ messageId+" by "+systemId+", destination="+destination+", sourceAddress="+sourceAddress.getAddress());
                                    }
                                } catch (WrongLengthOfStringException wlose) {
                                    LOGGER.log(WARNING, "Unable to process new source address: " + destinationAddress.debugString(), wlose);
                                }
                            }
                            submit.setSourceAddr(destinationAddress);
                            submit.setDestAddr(sourceAddress);
                        }
                         
                        deliverUsingStat(this, deliveryInfoEntry, DELIVERED);
                        break;
                    case "ENTEL":
                    case "CLARO":
                    case "VTR":
                    case "wom2rdvss":
                    //case "hablaip2022":
                        //LOGGER.log(SEVERE, "CGF doing 'deliveryInfoEntry, ACCEPTED' for message {0} by {1} processor", new Object[]{messageId, systemId});                                
                        deliverUsingStat(this, deliveryInfoEntry, ACCEPTED);
                        break;
                        
                    case "hablaip2022":
                        LOGGER.log(SEVERE, "CGF doing 'deliveryInfoEntry, DELIVERED' for message {0} by {1} processor", new Object[]{messageId, systemId});                                
                        deliverUsingStat(this, deliveryInfoEntry, DELIVERED);
                        break;
                        
                    default:
                        try (Connection conn = m_ds.getConnection()) {
                             String dlr_resp = "";                           
                            //LOGGER.log(WARNING, "CGF Checking messageId=" + messageId+" by systemID="+systemId);                           
                            //try (PreparedStatement ps = conn.prepareStatement("select t.id_report from FS_SMSCGW t where t.msgid in ( select r.msgid from FS_SMSCGW r where r.sender = ? minus select s.msgid from FS_SMSCGWNOTIFY s where s.sender = ?) and MSGID = ? AND RECEPTION_DATE > SYSDATE - 1.2 order by t.reception_date desc")) { //do not look for entries older than one day but give some extra time
                            String sql = "select t.id_report from FS_SMSCGW t where MSGID = ? AND  t.msgid in ( select r.msgid from FS_SMSCGW r where MSGID = ? and r.sender = ?   minus select s.msgid from FS_SMSCGWNOTIFY s where s.sender = ? and MSGID = ? ) and RECEPTION_DATE > SYSDATE - 1.2 order by t.reception_date desc";  //do not look for entries older than one day but give some extra time
                            try (PreparedStatement ps = conn.prepareStatement(sql)) 
                            {
                                ps.setString(1, messageId);
                                ps.setString(2, messageId);
                                ps.setString(3, systemId);
                                ps.setString(4, systemId);
                                ps.setString(5, messageId);

                            //sql = "select t.id_report, t.last_dlr, nvl(t.dlr_resp,'') DLRRESP from FS_SMSCGW t where t.sender = '"+systemId+"' AND t.MSGID = '"+messageId +"' AND t.RECEPTION_DATE > SYSDATE - 1.2 AND (t.DLR_RESP is NULL)  order by t.reception_date desc";
                            //LOGGER.log(WARNING, "CGF Busco el msg original=" + sql);
                            //try (PreparedStatement ps = conn.prepareStatement(sql)) 
                            //{                                                         
                            //try (PreparedStatement ps = conn.prepareStatement("select t.id_report from FS_SMSCGW t where t.msgid in ( select r.msgid from FS_SMSCGW r where r.sender = ? minus select s.msgid from FS_SMSCGWNOTIFY s where s.sender = ?) and MSGID = ? AND RECEPTION_DATE > SYSDATE - 1.2 order by t.reception_date desc")) { //do not look for entries older than one day but give some extra time
                            //    ps.setString(1, systemId);
                            //    //ps.setString(2, messageId);
                            //    ps.setString(2, systemId);
                            //    ps.setString(3, messageId);
                            
                                ResultSet rs = ps.executeQuery();
                                if (rs.next()) 
                                {
                                    final String status = rs.getString("ID_REPORT");
                                    //dlr_resp = ""+rs.getString("DLRRESP");                                   
                                    //LOGGER.log(WARNING,"DLR_RESP es: "+dlr_resp+", id_report="+status);
                                    
                                    if (ReportState.INITIAL.getState().equals(status)) {
                                        boolean retry = System.currentTimeMillis() - deliveryInfoEntry.submitted < TimeUnit.DAYS.toMillis(1);// Insist during only first day after object was created
                                        if (retry) {
                                            LOGGER.log(FINEST, "Requeuing tracking object identified by {0}", messageId);
                                            delayQueue.add(deliveryInfoEntry.updateDelay());
                                        } else {
                                            LOGGER.warning(() -> String.format("Time window to provide a deliver reply for FS_SMSCGW.MSGID={%s} has expired", messageId));
                                            registerDeliveryInfoBeforeConfirmingDelivery(conn, this, systemId, deliveryInfoEntry, EXPIRED);
                                        }
                                    } else {
                                        LOGGER.log(FINE, "Status for message {0} at processor {1} is {2}", new Object[]{messageId, systemId, status});
                                        
                                        if (ReportState.ERROR.getState().equals(status)) {
                                            LOGGER.finer(() -> String.format("Something happened while processing FS_SMSCGW.MSGID={%s}", messageId));
                                            registerDeliveryInfoBeforeConfirmingDelivery(conn, this, systemId, deliveryInfoEntry, UNDELIVERABLE);
                                            
                                        } else if (ReportState.OK.getState().equals(status)) {
                                            LOGGER.info(() -> String.format("Deliver received for FS_SMSCGW.MSGID={%s}", messageId));
                                            registerDeliveryInfoBeforeConfirmingDelivery(conn, this, systemId, deliveryInfoEntry, DELIVERED);
                                            
                                        } else if (ReportState.ABANDON.getState().equals(status)) {
                                            LOGGER.finer(() -> String.format("Retry exceeded while processing FS_SMSCGW.MSGID={%s}", messageId));
                                            registerDeliveryInfoBeforeConfirmingDelivery(conn, this, systemId, deliveryInfoEntry, EXPIRED);
                                            
                                        } else {
                                            LOGGER.finer(() -> String.format("Status was %s for FS_SMSCGW.MSGID={%s}", status, messageId));
                                            registerDeliveryInfoBeforeConfirmingDelivery(conn, this, systemId, deliveryInfoEntry, UNKNOWN);
                                        }
                                    }
                                } else {
                                    //LOGGER.warning(() -> String.format("Missing entry when checking status for FS_SMSCGW.MSGID={%s}", messageId));
                                    registerDeliveryInfoBeforeConfirmingDelivery(conn, this, systemId, deliveryInfoEntry, UNKNOWN);
                                }
                            }
                            //LOGGER.log(WARNING,"Procese query para DLR_RESP de messageId="+messageId);  
                            //if ("".equals(dlr_resp))
                            {
                                final long sleepTime;
                                //if ((sleepTime = TimeUnit.NANOSECONDS.toMillis(lastMomentDBWasHit + TimeUnit.SECONDS.toNanos(1) - System.nanoTime())) > 0) {//Hit DB just once every period
                                if (Integer.parseInt(DLR_TIME)>1000)
                                    sleepTime = TimeUnit.NANOSECONDS.toMillis(lastMomentDBWasHit + TimeUnit.SECONDS.toNanos(Integer.parseInt(DLR_TIME)/1000) - System.nanoTime());//Hit DB just once every period
                                else 
                                    sleepTime = TimeUnit.NANOSECONDS.toMillis(lastMomentDBWasHit + TimeUnit.MILLISECONDS.toNanos(Integer.parseInt(DLR_TIME)) - System.nanoTime());
                            
                                if (sleepTime > 0) {//Hit DB just once every period
                                    //LOGGER.log( WARNING,"Will sleep for {0} [ms]", sleepTime);
                                    Thread.sleep(sleepTime);                                                          
                                } // else 
                                //    LOGGER.log(WARNING,"DLR_RESP es NO null ("+dlr_resp+"), no necesito seguir con messageId="+messageId);  
                            }

                        } catch (SQLException e) {
                            LOGGER.log(SEVERE, e, () -> "Unexpected DB exception");
                        } finally {
                            lastMomentDBWasHit = System.nanoTime();
                        }
                        break;
                }
            } else {
                LOGGER.warning(() -> String.format("Processor is innactive for {%s}", systemId));
            }
        } catch (InterruptedException ie) {
            LOGGER.log(WARNING, ie, () -> "Unexpected interruption");
            Thread.currentThread().interrupt();
        }
    }

    private static void deliverUsingStat(DBDeliveryInfoSender deliveryInfoSender, DeliveryInfoEntry deliveryInfoEntry, int stat) {
        try {
            deliveryInfoEntry.stat = stat;
            
            deliveryInfoSender.deliver(deliveryInfoEntry);
        } catch (Exception ex) {
            LOGGER.log(SEVERE, "Unexpected exception while processing delivery", ex);
        }
    }

    private static void registerDeliveryInfoBeforeConfirmingDelivery(Connection conn, DBDeliveryInfoSender deliveryInfoSender, String systemId, DeliveryInfoEntry deliveryInfoEntry, int stat) {
        try {                
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO FS_SMSCGWNOTIFY(MSGID,ITERATOR,DATE_LOG,SENDER) VALUES(?,?,localtimestamp,?)")) {
                conn.setAutoCommit(false);                
                ps.setString(1, deliveryInfoEntry.messageId);
                ps.setString(2, String.valueOf(deliveryInfoEntry.submit.getSequenceNumber()));
                ps.setString(3, systemId);
                ps.execute();
                
            deliverUsingStat(deliveryInfoSender, deliveryInfoEntry, stat);

                conn.commit();
                            
             } catch (SQLException e) {
                LOGGER.log(SEVERE, e, () -> String.format("Unexpected DB exception for {%s}", systemId));
                try {
                    conn.rollback();
                } catch (SQLException re) {
                    LOGGER.log(SEVERE, re, () -> String.format("Unexpected exception while while trying to rollback changes for {%s}", systemId));
                }
            } finally {
                conn.setAutoCommit(true);
            }
/*
            String sql = "";
            sql = "UPDATE FS_SMSCGW set LAST_DLR=localtimestamp,SEQ_NUMBER="+deliveryInfoEntry.submit.getSequenceNumber()+"   WHERE MSGID='"+deliveryInfoEntry.messageId+"' and sender='"+systemId+"' and (DLR_RESP is NULL)";
            LOGGER.log(WARNING, "Ejecutare sql="+sql);
            sql = "UPDATE FS_SMSCGW set LAST_DLR=localtimestamp,SEQ_NUMBER=?   WHERE MSGID=?   AND   sender=? and (DLR_RESP is NULL)";
            try (PreparedStatement ps2 = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);  
                ps2.setString(1, String.valueOf(deliveryInfoEntry.submit.getSequenceNumber()));
                ps2.setString(2, deliveryInfoEntry.messageId);
                ps2.setString(3, systemId);
                ps2.execute();
                conn.commit();
 
            } catch (SQLException e) {
                LOGGER.log(SEVERE, e, () -> String.format("Unexpected DB exception for {%s}", systemId));
                try {
                    conn.rollback();
                } catch (SQLException re) {
                    LOGGER.log(SEVERE, re, () -> String.format("Unexpected exception while while trying to rollback changes for {%s}", systemId));
                }
            } finally {
                conn.setAutoCommit(true);
            }
*/            
        } catch (Exception ex) {
            LOGGER.log(SEVERE, "Unexpected exception while processing DLR", ex);
        }
    }

    @Override
    public void stop() {
        super.stop();//Stop processing first
        delayQueue.forEach((deliveryInfoEntry) -> {//Drains queue
            deliverUsingStat(this, deliveryInfoEntry, UNKNOWN);//sequence will be resetted. Will never know for sure what happened with message in queue 
        });
    }

    protected final class DBDeliveryInfoEntry extends DeliveryInfoSender.DeliveryInfoEntry implements Delayed {

        private long delay = getNextTimeout();

        public DBDeliveryInfoEntry(PDUProcessor processor, SubmitSM submit, int stat, int err, String messageId) {
            super(processor, submit, stat, err, messageId);
        }

        private long getNextTimeout() {
            return System.nanoTime() + ThreadLocalRandom.current().nextLong(LOWER_DELAY, UPPER_DELAY);
        }

        protected DBDeliveryInfoEntry updateDelay() {
            delay = getNextTimeout();
            return this;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(delay - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return o == this ? 0
                    : Long.compare(this.getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));
        }

    }
}
