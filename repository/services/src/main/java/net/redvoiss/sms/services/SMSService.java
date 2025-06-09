package net.redvoiss.sms.services;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.UUID;

import net.redvoiss.sms.bean.Destination;
import net.redvoiss.sms.services.bean.BatchReply;
import net.redvoiss.sms.services.bean.BatchStatus;
import net.redvoiss.sms.services.bean.BulkMessage;
import net.redvoiss.sms.services.bean.Message;
import net.redvoiss.sms.services.bean.MessageReply;
import net.redvoiss.sms.services.bean.MessageStatus;
import net.redvoiss.sms.services.error.SMSError;

import javax.sql.DataSource;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

public interface SMSService {

    public static final int MAX_SMS_LEN   = 480;

    static final Logger LOGGER = Logger.getLogger(SMSService.class.getName(), SMSService.class.getName());

    default String mapStatus(int state) {
        String ret = null;
        switch (state) {
            case 6:// FALLIDO
                ret = "FALLIDO";
                break;
            case 5:// RECIBIDO
                ret = "RECIBIDO";
                break;
            case 7:// ENVIADO SIN CONFIRMACION
            case 4:// ENVIADO
            case 3:// ENVIADO
                ret = "ENVIADO";
                break;
            case 2:// ENVIO PENDIENTE
            case 1:// ENVIO PENDIENTE
                ret = "ENVIO PENDIENTE";
                break;
            default:
                break;
        }
        return ret;
    }

    default MessageStatus checkMessageStatus(int messageId, String username, DataSource ds) throws SMSError {
        MessageStatus ret = null;
        try (java.sql.Connection conn = ds.getConnection()) {
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT LM.ESTADO, LI.ID_CLIENTE, LM.FECHA_NOTIFICACION FROM FC_SMS_LOTE_MENSAJE LM, FC_SMS_LOTE L, FC_SMS_LOTE_ITEM LI, FC_USUARIO U WHERE L.USUA_COD = U.USUA_COD AND LM.LOTE_COD = L.LOTE_COD AND LM.MENS_COD = ? AND U.USERNAME = ? AND LI.LOTE_COD=L.LOTE_COD ")) {                    
                    //"SELECT LM.ESTADO FROM FC_SMS_LOTE_MENSAJE LM, FC_SMS_LOTE L, FC_USUARIO U WHERE L.USUA_COD = U.USUA_COD AND LM.LOTE_COD = L.LOTE_COD AND LM.MENS_COD = ? AND U.USERNAME = ?")) {
                ps.setInt(1, messageId);
                ps.setString(2, username);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        final int state = rs.getInt("ESTADO");
                        final String idcliente = rs.getString("ID_CLIENTE");
                        final String fechotif = rs.getString("FECHA_NOTIFICACION");
                        ret = new MessageStatus(messageId, mapStatus(state), idcliente, fechotif);
                        LOGGER.log(SEVERE, () -> String.format("Obtuve el status y idcliente {%s} {%s}",
                                  mapStatus(state), idcliente));
                    }
                }
            }
        } catch (java.sql.SQLException sqle) {
            LOGGER.log(SEVERE, sqle, () -> String.format("DB exception while checking message reply of {%s} for {%s}",
                    messageId, username));
        }
        return ret;
    }

    default List<MessageReply> checkMessageReply(int messageId, String username, DataSource ds) throws SMSError {
        List<MessageReply> ret = new ArrayList<>();
        try (java.sql.Connection conn = ds.getConnection()) {
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT R.ID_REPLY as ID_REPLY, R.TEXT as TEXT, R.SEND_DATE as SEND_DATE, LI.ID_CLIENTE FROM FS_SMS_REPLIES R, FC_SMS_LOTE_MENSAJE LM, FC_SMS_LOTE L, FC_USUARIO, FC_SMS_LOTE_ITEM LI U WHERE LM.LOTE_COD = R.ID_LOTE AND LM.DESTINO = R.SOURCE_ADDRESS AND LM.LOTE_COD = L.LOTE_COD AND L.USUA_COD = U.USUA_COD AND LM.MENS_COD = ? AND U.USERNAME = ? AND LI.LOTE_COD=L.LOTE_COD ")) {
            //        "SELECT R.ID_REPLY as ID_REPLY, R.TEXT as TEXT, R.SEND_DATE as SEND_DATE FROM FS_SMS_REPLIES R, FC_SMS_LOTE_MENSAJE LM, FC_SMS_LOTE L, FC_USUARIO U WHERE LM.LOTE_COD = R.ID_LOTE AND LM.DESTINO = R.SOURCE_ADDRESS AND LM.LOTE_COD = L.LOTE_COD AND L.USUA_COD = U.USUA_COD AND LM.MENS_COD = ? AND U.USERNAME = ?")) {
                ps.setInt(1, messageId);
                ps.setString(2, username);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final int id = rs.getInt("ID_REPLY");
                        final java.sql.Date date = rs.getDate("SEND_DATE");
                        final String message = rs.getString("TEXT");
                        final String idcliente = rs.getString("ID_CLIENTE");
                        
                        ret.add(new MessageReply(id, new Date(date.getTime()), message, idcliente));
                    }
                }
            }
        } catch (java.sql.SQLException sqle) {
            LOGGER.log(SEVERE, sqle, () -> String.format("DB exception while checking message reply of {%s} for {%s}",
                    messageId, username));
        }
        return ret;
    }

    default BatchReply checkBatchReplies(int batchId, String username, DataSource ds) throws SMSError {
        List<MessageReply> messageReplyList = new ArrayList<>();
        BatchReply ret = new BatchReply(batchId, messageReplyList);
        try (java.sql.Connection conn = ds.getConnection()) {
            String sql = "SELECT R.ID_REPLY as ID_REPLY, R.TEXT as TEXT, R.SEND_DATE as SEND_DATE, LI.ID_CLIENTE "+ 
                         "FROM FS_SMS_REPLIES R, FC_SMS_LOTE L, FC_USUARIO U, FC_SMS_LOTE_ITEM LI "+
                         "WHERE U.USERNAME = ? AND L.USUA_COD = U.USUA_COD AND L.LOTE_COD = ? AND "+
                         "      R.ID_LOTE = L.LOTE_COD AND LI.LOTE_COD=L.LOTE_COD ";

            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {            
            
            //        "SELECT R.ID_REPLY as ID_REPLY, R.TEXT as TEXT, R.SEND_DATE as SEND_DATE FROM FS_SMS_REPLIES R, FC_SMS_LOTE L, FC_USUARIO U WHERE L.USUA_COD = U.USUA_COD AND L.LOTE_COD = R.ID_LOTE AND L.LOTE_COD = ? AND U.USERNAME = ?")) {
            //try (java.sql.PreparedStatement ps = conn.prepareStatement(
            //        "SELECT R.ID_REPLY as ID_REPLY, R.TEXT as TEXT, R.SEND_DATE as SEND_DATE, LI.ID_CLIENTE FROM FS_SMS_REPLIES R, FC_SMS_LOTE L, FC_USUARIO U, FC_SMS_LOTE_ITEM LI WHERE L.USUA_COD = U.USUA_COD AND L.LOTE_COD = R.ID_LOTE AND L.LOTE_COD = ? AND U.USERNAME = ? AND LI.LOTE_COD=L.LOTE_COD ")) {
                ps.setInt(2, batchId);
                ps.setString(1, username);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final int id = rs.getInt("ID_REPLY");
                        final java.sql.Date date = rs.getDate("SEND_DATE");
                        final String message = rs.getString("TEXT");
                        final String idcliente = rs.getString("ID_CLIENTE");
                        
                        messageReplyList.add(new MessageReply(id, new Date(date.getTime()), message, idcliente));
                    }
                }
            }
        } catch (java.sql.SQLException sqle) {
            LOGGER.log(SEVERE, sqle, () -> String
                    .format("DB exception while checking message reply for batch {%s} for {%s}", batchId, username));
        }
        return ret;
    }

    default List<BatchStatus> checkBatchStatus(int batchId, String username, DataSource ds) throws SMSError {
        LOGGER.info(() -> String.format("Will check batch id {%s} for user {%s}", batchId, username));
        try (java.sql.Connection conn = ds.getConnection()) {
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT L.USUA_COD FROM FC_SMS_LOTE L, FC_USUARIO U WHERE L.LOTE_COD = ? AND L.USUA_COD = U.USUA_COD AND U.USERNAME = ?")) {
                ps.setInt(1, batchId);
                ps.setString(2, username);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        LOGGER.fine(
                                () -> String.format("Batch id {%s} seems to belong to user {%s}", batchId, username));
                    } else {
                        LOGGER.warning(() -> String.format(
                                "User {%s} tried to check batch id {%s} that seems to belong to different user",
                                username, batchId));
                        throw new SMSError("Unauthorized");
                    }
                }
            }
            List<BatchStatus> ret = new ArrayList<>();
            //        "select MENS_COD, SMS_COD, DESTINO, ESTADO from FC_SMS_LOTE_MENSAJE where LOTE_COD = ?")) {
            String sql = "select LM.MENS_COD, LM.SMS_COD, LM.DESTINO, LM.ESTADO, LI.ID_CLIENTE, LM.FECHA_NOTIFICACION " +
                         "from FC_SMS_LOTE_MENSAJE LM, FC_SMS_LOTE LO, FC_SMS_LOTE_ITEM LI " +
                         "where LO.LOTE_COD = ? AND LM.LOTE_COD = LO.LOTE_COD AND LI.LOTE_COD=LM.LOTE_COD " +
                         " AND LI.DESTINO=LM.DESTINO ";

            //         "select LM.MENS_COD, LM.SMS_COD, LM.DESTINO, LM.ESTADO, LI.ID_CLIENTE from FC_SMS_LOTE_MENSAJE LM, FC_SMS_LOTE_ITEM LI where LM.LOTE_COD = ? AND LI.LOTE_COD=LM.LOTE_COD ")) {

            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, batchId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final int state = rs.getInt("ESTADO");
                        final String destination = rs.getString("DESTINO");
                        final int msgCod = rs.getInt("MENS_COD");
                        final int smsCod = rs.getInt("SMS_COD");
                        final String status = mapStatus(state);
                        final String idcliente = rs.getString("ID_CLIENTE");
                        final String fechotif = rs.getString("FECHA_NOTIFICACION");

                        if (status == null) {
                            LOGGER.warning(() -> String.format("Unexpected state {%d} for {%s} with SMS Code {%d}",
                                    state, destination, smsCod));
                        } else {
                            ret.add(new BatchStatus(msgCod, destination, status, idcliente, fechotif));
                        }
                    }
                }
            }            
            /*
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "select MENS_COD, SMS_COD, DESTINO, ESTADO from FC_SMS_LOTE_MENSAJE where LOTE_COD = ?")) {
                ps.setInt(1, batchId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final int state = rs.getInt("ESTADO");
                        final String destination = rs.getString("DESTINO");
                        final int msgCod = rs.getInt("MENS_COD");
                        final int smsCod = rs.getInt("SMS_COD");
                        final String status = mapStatus(state);
                        if (status == null) {
                            LOGGER.warning(() -> String.format("Unexpected state {%d} for {%s} with SMS Code {%d}",
                                    state, destination, smsCod));
                        } else {
                            ret.add(new BatchStatus(msgCod, destination, status));
                        }
                    }
                }
            }*/
            return ret;
        } catch (java.sql.SQLException e) {
            LOGGER.log(SEVERE, e,
                    () -> String.format("DB exception while checking batch {%d} status for {%s}", batchId, username));
        }
        return Collections.emptyList();
    }

    default String send(final String username, BulkMessage bulkMessage, DataSource dataSource, boolean reply)
            throws SMSError {
        String ret = null;
        String fallidos = ""; /* CGF, 20200910. It will contain mobiles that fail the isOK() validation */
        final long startTime = System.nanoTime();
        if (bulkMessage == null) {
            LOGGER.severe("No data");
            throw new SMSError("No data found to process");
        } else if (bulkMessage.getBulkName() == null) {
            LOGGER.severe("Bulk name missing");
            throw new SMSError("Bulk name is missing");
        } else if (bulkMessage.getMessage() == null || bulkMessage.getMessage().isEmpty()) {
            LOGGER.severe("Message missing");
            throw new SMSError("Message is missing");
        } else {
           //LOGGER.severe("New SMS processing:"+bulkMessage.toString());
            if (dataSource == null) {
                LOGGER.warning("DS is null");
            } else {
                try (Connection conn = dataSource.getConnection()) {
                    //LOGGER.warning("Got a new connection");
                    Message[] arr = bulkMessage.getMessageDetails();
                    //LOGGER.warning("Built arr variable, length: "+(arr!=null ? arr.length : -1));
                    if (arr == null) {
                        LOGGER.severe("No details");
                        throw new SMSError("Message details are missing");
                    } else {
                        for (int i = 0; i < arr.length; i++) {
                            Message m = arr[i];
                            if (m == null) {
                                LOGGER.warning("Discarding message due to detail is missing");
                            } else if (m.getDestination() == null) {
                                LOGGER.warning("Discarding message due to destination is missing");
                            } else {
                                Destination destination = new Destination(m.getDestination());
                                
                                String msg = bulkMessage.getMessage(); //new String (bulkMessage.getMessage().getBytes(),"ISO-8859-1");
                                
                                if (!"".equals(msg) && destination.isOK_v2()) {
                                    if (msg.length()>MAX_SMS_LEN)
                                       msg = ""+msg.substring(0, MAX_SMS_LEN );
                                    
                                    //DateFormat DATE_FORMATTER = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");                                    
                                    //String fecha0 = ""+bulkMessage.getSendDate();                               
                                    //LOGGER.warning("Fecha0 = '"+fecha0+"' ");
                                    //if ("".equals(fecha0))
                                    //    fecha0 = DATE_FORMATTER.format(new Date());
                                    //else
                                    //    fecha0 = DATE_FORMATTER.format(bulkMessage.getSendDate());                                
                                    //LOGGER.warning("Sending to :"+m.getDestination()+", fecha envio:"+fecha0) ; 
                                
                                    try {
                                        // CGF, 20201002. Corrijo cargo del lote si han habido fallidos previo al utimo destino.
                                        // Para lo cual, en caso de que (i + 1 == arr.length && !"".equals(fallidos)) se cumpla: 
                                        // FUERZO llamada a creaDespachaLote sin indicador de ultimo
                                        // para que NO se llame al store procedure carga_usuario, que aplica cargo al usuario.
                                        if (i + 1 == arr.length && !"".equals(fallidos)) {
                                            if (reply) {
                                                ret = net.redvoiss.sms.dao.DAO.creaDespachaLoteCompletoReply(
                                                        conn, username, destination.getTarget(),
                                                        msg, "", ret, false, //i + 1 == arr.length,
                                                        bulkMessage.getBulkName(), m.getField(),
                                                        bulkMessage.isCommercial(),
                                                        bulkMessage.getSendDate() == null ? new Date()
                                                        : bulkMessage.getSendDate(),
                                                        m.idCliente());
                                            } else {
                                                ret = net.redvoiss.sms.dao.DAO.creaDespachaLoteCompleto(conn,
                                                        username, destination.getTarget(), 
                                                        msg, "", ret, false, //i + 1 == arr.length,
                                                        bulkMessage.getBulkName(),
                                                        m.getField(), bulkMessage.isCommercial(),
                                                        bulkMessage.getSendDate() == null ? new Date()
                                                        : bulkMessage.getSendDate(),
                                                        m.idCliente());
                                            }
                                        } else {
                                            // CGF, fin correccion cargo ante fallidos                                              
                                            if (reply) {
                                                ret = net.redvoiss.sms.dao.DAO.creaDespachaLoteCompletoReply(
                                                        conn, username, destination.getTarget(),
                                                        msg, "", ret, i + 1 == arr.length,
                                                        bulkMessage.getBulkName(), m.getField(),
                                                        bulkMessage.isCommercial(),
                                                        bulkMessage.getSendDate() == null ? new Date()
                                                        : bulkMessage.getSendDate(),
                                                        m.idCliente());
                                            } else {
                                                ret = net.redvoiss.sms.dao.DAO.creaDespachaLoteCompleto(conn,
                                                        username, destination.getTarget(), 
                                                        msg, "", ret, i + 1 == arr.length, bulkMessage.getBulkName(),
                                                        m.getField(), bulkMessage.isCommercial(),
                                                        bulkMessage.getSendDate() == null ? new Date()
                                                        : bulkMessage.getSendDate(),
                                                        m.idCliente());
                                            }
                                        }
                                    } catch (SQLException sqle) {
                                        /**
                                         * We cannot simply avoid calling these
                                         * methods when 'field' if unavailable,
                                         * as this is an alternative parameter.
                                         * Unfortunately, it is the combination
                                         * of 'message' and 'field', being
                                         * replaced at DB side, that fails. Ref:
                                         * LAEL-168
                                         */
                                        
                                        if (sqle.getErrorCode() == 20001) {//Balance too low
                                                LOGGER.log(WARNING, String.format("Balance too low to store message to %s", username), sqle);
                                        }
                                        else if (sqle.getErrorCode() == 20002) {//Mobile number is on Blacklist
                                                LOGGER.log(WARNING, String.format("Mobile %s is on blacklist", destination), sqle);
                                        }                                        
                                        
                                        LOGGER.log(SEVERE, String.format(
                                                "Unexpected DB exception while processing message sent to {%s}. Message is {%s}, using parameter {%s}",
                                                destination.getScrambledTarget(), bulkMessage.getMessage(),
                                                m.getField()), sqle);
                                        throw sqle;
                                    }
                                } else {
                                    
                                    //if ("".equals(msg) || msg.length()>MAX_SMS_LEN)  // Fallo porque texto del SMS pasa el limite de MAX_SMS_LEN=160
                                    //{
                                    //    LOGGER.warning(() -> String.format(
                                    //        "Discarding destination {%s} due to message length ({%d} > {%d})", m.getDestination(), msg.length(), MAX_SMS_LEN));
                                    //} else {  // Si no, Asumo que  el destino es invalido
                                        LOGGER.warning(() -> String.format(
                                            "Discarding message due to invalid destination format: {%s}", m.getDestination()));
                                    //}
                                    /* CGF, 20200910. I add to 'fallidos' list those destinations that fail isOK() and isOK_v2() call */
                                    fallidos = "".equals(fallidos) ? m.getDestination() : fallidos+","+m.getDestination();
                                    
                                    // CGF, 2020-10-20. Que sean registrados los fallidos pero con estado que indique fallido                                              
                                    if (reply) {
                                        ret = net.redvoiss.sms.dao.DAO.creaDespachaLoteConItemFallidoReply(
                                                    conn, username, destination.getTarget(),
                                                    bulkMessage.getMessage(), "", ret, i + 1 == arr.length, 
                                                    bulkMessage.getBulkName(), m.getField(),
                                                    bulkMessage.isCommercial(),
                                                    bulkMessage.getSendDate() == null ? new Date()
                                                    : bulkMessage.getSendDate());
                                    } else {
                                        ret = net.redvoiss.sms.dao.DAO.creaDespachaLoteConItemFallido(conn,
                                                    username, destination.getTarget(), bulkMessage.getMessage(),
                                                    "", ret, i + 1 == arr.length, bulkMessage.getBulkName(),
                                                    m.getField(), bulkMessage.isCommercial(),
                                                    bulkMessage.getSendDate() == null ? new Date()
                                                    : bulkMessage.getSendDate());
                                    }
                                    // Fin.                                    
                                }
                            }
                        }
                    }
                } catch (SQLException sqle) {
                    final String msg = String.format("Unexpected DB exception referenced by {%s}",
                            UUID.randomUUID().toString());
                    LOGGER.log(SEVERE, msg, sqle);
                    throw new SMSError(msg);
                } catch (Exception e) {
                    final String msg = String.format("Unexpected exception referenced by {%s}",
                            UUID.randomUUID().toString());
                    LOGGER.log(SEVERE, msg, e);
                    throw new SMSError(msg);
                } finally {
                    final long elapsedTime = System.nanoTime() - startTime;
                    LOGGER.fine(() -> String.format("Elapsed time while processing batch for user {%s} was {%d}", username,
                            NANOSECONDS.convert(elapsedTime, SECONDS)));
                }
            }

        }
        if (ret == null) {
            LOGGER.warning(
                    () -> String.format("Bulk failed to be generated for {%s}", username));
        } else {
            LOGGER.info(String.format("Finished processing bulk: {%s} for {%s}", ret,
                    username));
        }

        return ret;
    }
    
    default String send_v2(final String username, BulkMessage bulkMessage, DataSource dataSource, boolean reply)
            throws SMSError {
        String ret = null;
        String fallidos = ""; /* CGF, 20200910. It will contain destinations that fail isOK() validation */
        final long startTime = System.nanoTime();
        LOGGER.info("Inside send_v2 at :"+startTime);
        if (bulkMessage == null) {
            LOGGER.severe("No data");
            throw new SMSError("No data found to process");
        } else if (bulkMessage.getBulkName() == null) {
            LOGGER.severe("Bulk name missing");
            throw new SMSError("Bulk name is missing");
        } else if (bulkMessage.getMessage() == null || bulkMessage.getMessage().isEmpty()) {
            LOGGER.severe("Message missing");
            throw new SMSError("Message is missing");
        } else {
            LOGGER.fine(bulkMessage.toString());
            if (dataSource == null) {
                LOGGER.warning("DS is null");
            } else {
                try (Connection conn = dataSource.getConnection()) {
                    LOGGER.fine("Got a new connection");
                    Message[] arr = bulkMessage.getMessageDetails();
                    if (arr == null) {
                        LOGGER.severe("No details");
                        throw new SMSError("Message details are missing");
                    } else {
                        for (int i = 0; i < arr.length; i++) {
                            Message m = arr[i];
                            if (m == null) {
                                LOGGER.warning("Discarding message due to detail is missing");
                            } else if (m.getDestination() == null) {
                                LOGGER.warning("Discarding message due to destination is missing");
                            } else {
                                Destination destination = new Destination(m.getDestination());                                
                                LOGGER.info("Sending with send_v2 to :"+m.getDestination());
                                
                                // CGF 2020911. I use new isOK2 function to validate lengths of MSISDN
                                if (destination.isOK_v2()) {
                                    try {
                                        // CGF, 20201002. Corrijo cargo del lote si han habido fallidos previo al utimo destino.
                                        // Para lo cual, en caso de que (i + 1 == arr.length && !"".equals(fallidos)) se cumpla: 
                                        // FUERZO llamada a creaDespachaLote sin indicador de ultimo
                                        // para que NO se llame al store procedure carga_usuario, que aplica cargo al usuario.
                                        if (i + 1 == arr.length && !"".equals(fallidos)) {
                                            if (reply) {
                                                ret = net.redvoiss.sms.dao.DAO.creaDespachaLoteCompletoReply(
                                                        conn, username, destination.getTarget(),
                                                        bulkMessage.getMessage(), "", ret, false, //i + 1 == arr.length,
                                                        bulkMessage.getBulkName(), m.getField(),
                                                        bulkMessage.isCommercial(),
                                                        bulkMessage.getSendDate() == null ? new Date()
                                                        : bulkMessage.getSendDate(), "");
                                            } else {
                                                ret = net.redvoiss.sms.dao.DAO.creaDespachaLoteCompleto(conn,
                                                        username, destination.getTarget(), 
                                                        bulkMessage.getMessage(), "", ret, false, //i + 1 == arr.length,
                                                        bulkMessage.getBulkName(),
                                                        m.getField(), bulkMessage.isCommercial(),
                                                        bulkMessage.getSendDate() == null ? new Date()
                                                        : bulkMessage.getSendDate(), "");
                                            }
                                        } else {
                                        // CGF, fin correccion cargo ante fallidos                                              
                                            if (reply) {
                                                ret = net.redvoiss.sms.dao.DAO.creaDespachaLoteCompletoReply(
                                                        conn, username, destination.getTarget(),
                                                        bulkMessage.getMessage(), "", ret, i + 1 == arr.length,
                                                        bulkMessage.getBulkName(), m.getField(),
                                                        bulkMessage.isCommercial(),
                                                        bulkMessage.getSendDate() == null ? new Date()
                                                        : bulkMessage.getSendDate(), "");
                                            } else {
                                                ret = net.redvoiss.sms.dao.DAO.creaDespachaLoteCompleto(conn,
                                                        username, destination.getTarget(), bulkMessage.getMessage(),
                                                        "", ret, i + 1 == arr.length, bulkMessage.getBulkName(),
                                                        m.getField(), bulkMessage.isCommercial(),
                                                        bulkMessage.getSendDate() == null ? new Date()
                                                        : bulkMessage.getSendDate(), "");
                                            }
                                        }
                                    } catch (SQLException sqle) {
                                        /**
                                         * We cannot simply avoid calling these
                                         * methods when 'field' if unavailable,
                                         * as this is an alternative parameter.
                                         * Unfortunately, it is the combination
                                         * of 'message' and 'field', being
                                         * replaced at DB side, that fails. Ref:
                                         * LAEL-168
                                         */
                                        LOGGER.log(SEVERE, String.format(
                                                "Unexpected DB exception while processing message sent to {%s}. Message is {%s}, using parameter {%s}",
                                                destination.getScrambledTarget(), bulkMessage.getMessage(),
                                                m.getField()), sqle);
                                        throw sqle;
                                    }
                                } else {
                                    LOGGER.warning(() -> String.format(
                                            "Discarding message due to destination format: {%s}",
                                            m.getDestination()));
                                    /* CGF, 20200910. I add to 'fallidos' list those destinations that fail isOK() call */
                                    fallidos = "".equals(fallidos) ? m.getDestination() : fallidos+","+m.getDestination();
                                }
                            }
                        }
                    }
                } catch (SQLException sqle) {
                    final String msg = String.format("Unexpected DB exception referenced by {%s}",
                            UUID.randomUUID().toString());
                    LOGGER.log(SEVERE, msg, sqle);
                    throw new SMSError(msg);
                } catch (Exception e) {
                    final String msg = String.format("Unexpected exception referenced by {%s}",
                            UUID.randomUUID().toString());
                    LOGGER.log(SEVERE, msg, e);
                    throw new SMSError(msg);
                } finally {
                    final long elapsedTime = System.nanoTime() - startTime;
                    LOGGER.fine(() -> String.format("Elapsed time while processing batch for user {%s} was {%d}", username,
                            NANOSECONDS.convert(elapsedTime, SECONDS)));
                }
            }

        }
        if (ret == null) {
            LOGGER.warning(
                    () -> String.format("Bulk failed to be generated for {%s}", username));
        } else {
            LOGGER.info(String.format("Finished processing bulk: {%s} for {%s}", ret,
                    username));
        }
        /* CGF, 20200910. I change JSON output for including 'fallidos' list */
        ret = ret==null || "".equals(ret) ? "-1|"+fallidos : ret+"|"+fallidos;
        return ret;
    }
    
    
}
