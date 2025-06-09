package net.redvoiss.sms.smpp;

import org.smpp.smscsim.ShortMessageStore;
import org.smpp.smscsim.ShortMessageStoreException;

import org.smpp.Data;
import org.smpp.pdu.SubmitSM;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import net.redvoiss.sms.dao.DAO;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

public class ShortMessageDBStore extends ShortMessageStore {

    private static final Logger LOGGER = Logger.getLogger(ShortMessageDBStore.class.getName());
    private final DataSource m_ds;

    public ShortMessageDBStore(DataSource ds) throws SQLException {
        m_ds = ds;
    }
    protected static String incomingHablaIPDestination(String s) {
        if (s!=null && "389310".equals(s.substring(0,6)) && s.length()==10)
            return s.replaceFirst("^389310", "");
        else
            return s == null ? null : s.replaceFirst("^389310", "56");
    }
    
    protected static String incomingMovistarDestination(String s) {
        if (s!=null && "215310".equals(s.substring(0,6)) && s.length()==10)
            return s.replaceFirst("^215310", "");
        else
            return s == null ? null : s.replaceFirst("^215310", "56");
    }

    protected static String incomingEntelDestination(String s) {        
        if (s!=null && "220310".equals(s.substring(0,6)) && s.length()==10)
            return s.replaceFirst("^220310", "");
        else
            return s == null ? null : s.replaceFirst("^220310", "56").replaceFirst("^220", "564");
    }

    protected static String incomingVTRDestination(String s) {
        if (s!=null && "235310".equals(s.substring(0,6)) && s.length()==10)
            return s.replaceFirst("^235310", "");
        else
            return s == null ? null : s.replaceFirst("^235310", "56");
    }

    protected static String incomingWOMDestination(String s) {
        if (s!=null && "225310".equals(s.substring(0,6)) && s.length()==10)
            return s.replaceFirst("^225310", "");
        else
            return s == null ? null : s.replaceFirst("^225310", "56");
    }

    @Override
    public synchronized void submit(SubmitSM message, String messageId, String systemId) throws ShortMessageStoreException, UnsupportedEncodingException {
        if (systemId == null) {
            LOGGER.severe("System id is missing");
        } else if (message.getShortMessage() == null) {
            LOGGER.log(WARNING, "Discarding empty message {0} from {1}", new String[]{messageId, systemId});
            throw new ShortMessageStoreException(new Exception("Unexpected empty message"));
        } else if (Data.DFLT_DATA_CODING == message.getDataCoding() || 3 /* ISO-8859-1 */ == message.getDataCoding() || 8 /* UCS2 */ == message.getDataCoding()) {// Supported encoding

             LOGGER.log(WARNING, "CGF ---> Message de systemId="+systemId+", messageId="+messageId);

             try (Connection conn = m_ds.getConnection()) {
                try {
                    conn.setAutoCommit(false);
                    switch (systemId) {
                        case "MOVISTAR":
                            decodeAndStore(message, conn, messageId, systemId, 9, incomingMovistarDestination(message.getDestAddr().getAddress()), Data.ENC_CP1252);
                            break;
                        case "ENTEL":
                            String dest_cod = DAO.getDestino(conn,message.getDestAddr().getAddress());
                            LOGGER.warning("---> DAO.getDestino('"+message.getDestAddr().getAddress()+"') retorna:  "+dest_cod);
                            
                            if ("43".equals(dest_cod))
                            {
                                final String decodedMessage = message.getShortMessage(Data.ENC_CP1252);
                                String destaddress = message.getDestAddr().getAddress();
                                
                                if ((byte)message.getEsmClass()==0x40) {
                                    LOGGER.warning("En submit para dest_cod=43, SMS largo ");
                                    DAO.creaDespachaLSR_SMPP(conn, systemId, destaddress, message.getSourceAddr().getAddress(), decodedMessage, messageId, (short)1, message.getUdhHeader() );
                                } else {
                                    LOGGER.warning("En submit para dest_cod=43, SMS corto ");
                                    DAO.creaDespachaLoteSimpleReply(conn, systemId, destaddress, message.getSourceAddr().getAddress(), decodedMessage, messageId);
                                }
                            } else
                                decodeAndStore(message, conn, messageId, systemId, 1, incomingEntelDestination(message.getDestAddr().getAddress()), Data.ENC_CP1252);

                            break;
                        case "CLARO":
                        case "RVOIS_NC_TX":
                            decodeAndStore(message, conn, messageId, systemId, 2, message.getDestAddr().getAddress(), Data.ENC_CP1252);
                            break;
                        //case "hablaip2022":
                        //    decodeAndStore(message, conn, messageId, systemId, 38, incomingHablaIPDestination(message.getDestAddr().getAddress()), Data.ENC_ISO8859_1);
                        //    break;
                        case "VTR":
                            decodeAndStore(message, conn, messageId, systemId, 22, incomingVTRDestination(message.getDestAddr().getAddress()), Data.ENC_CP1252);
                            break;
                        case "wom2rdvss":
                            decodeAndStore(message, conn, messageId, "WOM", 26, incomingWOMDestination(message.getDestAddr().getAddress()), Data.ENC_GSM7BIT);
                            break;
                        default:
                            final String encoding;

                            switch (message.getDataCoding()) {
                                case Data.DFLT_DATA_CODING:
                                    switch (systemId) {
                                        case "nexmo":
                                            encoding = Data.ENC_CP1252;
                                            break;
                                        //case "hablaip2022":
                                        case "telecoch":
                                            encoding = Data.ENC_ISO8859_1;
                                            break;
                                        default:
                                            encoding = Data.ENC_GSM7BIT;
                                            break;
                                    }
                                    break;
                                case 0x11:
                                    encoding = Data.ENC_ISO8859_1;
                                    break;
                                default:
                                    //encoding = Data.ENC_UTF16;
                                    encoding = Data.ENC_ISO8859_1;
                                    break;
                            }

                            final String decodedMessage = message.getShortMessage(encoding);
                            String destaddress = message.getDestAddr().getAddress();
                            
                            if ("hablaip2022".equals(systemId))
                            {
                                destaddress = incomingHablaIPDestination(destaddress);
                                LOGGER.log(WARNING, "CGF: Message text {0} from {1}, decodedMessage queda en {2}, to {3}", 
                                        new String[]{message.getShortMessage(), systemId, decodedMessage, destaddress});
                            }
                            LOGGER.finest(() -> {
                                return String.format("Message {%s}, was decoded using {%s} due to coding value {%02x}: {%x}", 
                                        messageId, encoding, message.getDataCoding(), new BigInteger(message.getShortMessageData().getBuffer()));
                            });

                            if ((byte)message.getEsmClass()==0x40) {
                                //LOGGER.warning("En ShortMessageDBStore para SMS particionado: systemId="+systemId+", destino="+message.getDestAddr().getAddress()+", esmClass="+(short)message.getEsmClass()+", udhHeader="+message.getUdhHeader());
                                 DAO.creaDespachaLSR_SMPP(conn, systemId, destaddress, message.getSourceAddr().getAddress(), decodedMessage, messageId, (short)1, message.getUdhHeader() );
                            } else {
                                //LOGGER.warning("En ShortMessageDBStore para SMS NO particionado: systemId="+systemId+", destino="+message.getDestAddr().getAddress()+", origen="+message.getSourceAddr().getAddress()+", messageId="+messageId+", decodedMessage="+decodedMessage);
                                DAO.creaDespachaLoteSimpleReply(conn, systemId, destaddress, message.getSourceAddr().getAddress(), decodedMessage, messageId);
                            }                                

                            break;
                    }
                    conn.commit();
                } catch (UnsupportedEncodingException | SQLException e) {
                    try {
                        conn.rollback();
                    } catch (SQLException re) {
                        LOGGER.log(SEVERE, "SQL Exception during rollback", re);
                    }
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (Exception e) {
                LOGGER.log(SEVERE, String.format("Unexpected exception detected while processing message id {%s} for system {%s}", messageId, systemId), e);
                throw new ShortMessageStoreException(e);
            }
        } else {
            LOGGER.log(WARNING, "Discarding unexpected message {0} with encoding {1} from {2}", new Object[]{messageId, message.getDataCoding(), systemId});
            throw new ShortMessageStoreException(new UnsupportedEncodingException(String.valueOf(message.getDataCoding())));
        }
    }

    private static void decodeAndStore(SubmitSM message, final Connection conn, String messageId, String systemId, int route, String destination, String defaultEncoding) throws SQLException, UnsupportedEncodingException {
        try (PreparedStatement ps = conn.prepareStatement("insert into fs_smsc(msgid,sender,ruta_cod,sourceaddress,destaddress,message,reception_date,id_smsc) values (?,?,?,?,?,?,sysdate,1)")) {
            ps.setString(1, messageId);
            ps.setString(2, systemId);
            ps.setInt(3, route);
            ps.setString(4, message.getSourceAddr().getAddress());
            ps.setString(5, destination);
            
            final String shortMessage, encoding;
            switch (message.getDataCoding()) {
                case Data.DFLT_DATA_CODING:
                    encoding = defaultEncoding;
                    break;
                case 3://ISO-8859-1
                    encoding = Data.ENC_ISO8859_1;
                    break;
                case 8://UCS2
                    encoding = Data.ENC_UTF16;
                    break;
                default:
                    throw new UnsupportedEncodingException(String.format("Unexpected data coding %02x for %s from %s", message.getDataCoding(), messageId, systemId));
            }
            shortMessage = message.getShortMessage(encoding);
            LOGGER.finest(() -> {
                return String.format("Message {%s}, was decoded using {%s} due to coding value {%02x}: {%x}", messageId, encoding, message.getDataCoding(),
                        new BigInteger(message.getShortMessageData().getBuffer()));
            });
            LOGGER.log(SEVERE,"API HAGO insert into fs_smsc(msgid,sender,ruta_cod,sourceaddress,destaddress,message,reception_date,id_smsc) values ('"+messageId+"','"+systemId+"',"+route+",'"+message.getSourceAddr().getAddress()+"','"+destination+"','"+shortMessage+"',sysdate,1)");

            ps.setString(6, shortMessage);
            
            ps.executeQuery();
        }
    }

    @Override
    public void cancel(String messageId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replace(String messageId, String newMessage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String print() {
        throw new UnsupportedOperationException();
    }
}
