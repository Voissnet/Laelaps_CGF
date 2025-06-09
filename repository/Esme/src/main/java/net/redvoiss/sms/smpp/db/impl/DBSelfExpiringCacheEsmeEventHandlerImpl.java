package net.redvoiss.sms.smpp.db.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import net.redvoiss.sms.dao.DAO;
import net.redvoiss.sms.smpp.SubmitContext;
import net.redvoiss.sms.smpp.bean.Message;
import net.redvoiss.sms.smpp.bean.Message.MessageId;
import net.redvoiss.sms.smpp.cache.AbstractSelfExpiringCacheFactory;
import net.redvoiss.sms.smpp.db.SQLUtil;
import net.redvoiss.sms.smpp.dlr.AbstractDlrParserFactory;
import net.redvoiss.sms.smpp.dlr.DlrParser;
import net.redvoiss.sms.smpp.dlr.DlrParser.StatEnum;
import net.redvoiss.sms.smpp.impl.SelfExpiringCacheEsmeEventHandlerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smpp.Data;

/**
 *
 * @author Jorge Avila
 */
public class DBSelfExpiringCacheEsmeEventHandlerImpl extends SelfExpiringCacheEsmeEventHandlerImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(net.redvoiss.sms.smpp.impl.SelfExpiringCacheEsmeEventHandlerImpl.class);

    /**
     * Creates DB based handler
     *
     * @param abstractSelfExpiringCacheFactory
     * @param abstractDlrParserFactory
     */
    public DBSelfExpiringCacheEsmeEventHandlerImpl(AbstractSelfExpiringCacheFactory abstractSelfExpiringCacheFactory, AbstractDlrParserFactory abstractDlrParserFactory) {
        super(abstractSelfExpiringCacheFactory, abstractDlrParserFactory);
    }

    @Override
    protected void processSubmitResponse(int cStatus, int seqN, String messageId, Message.MessageId id) {
        if (Data.ESME_ROK == cStatus) {
            super.processSubmitResponse(cStatus, seqN, messageId, id);
        } else {
            LOGGER.debug("Handling submit error response: sequence number is {}, command status is {}, message id is {} and message was {}", seqN, cStatus, messageId, id.getPrimary());
            SQLUtil.execute(() -> {
                try (Connection conn = SQLUtil.getConnection()) {
                    boolean isStoreFaultyRecordOk = DAO.storeFaultyRecord(conn, id.getPrimary(), id.getSecondary(), 0);
                    if (isStoreFaultyRecordOk) {
                        LOGGER.trace("Faulty record stored: sequence number is {}, command status is {}, message id is {} and message was {}", seqN, cStatus, messageId, id.getPrimary());
                    } else {
                        LOGGER.warn("Unable to store faulty record: sequence number is {}, command status is {}, message id is {} and message was {}", seqN, cStatus, messageId, id.getPrimary());
                    }
                } catch (SQLException sqle) {
                    LOGGER.error("Unexpected DB exception", sqle);
                }
            });
        }
    }

    @Override
    protected boolean processDLR(MessageId messageId, DlrParser dlr) {
        boolean ret = false;
        String smsCode = messageId.getPrimary(), idGw = messageId.getSecondary();
        final Optional<StatEnum> stat = dlr.parseStatField();
        if (stat.isPresent()) {
            switch (stat.get()) {
                case ACCEPTD:
                    break;
                case UNKNOWN:
                case EXPIRED:
                case DELETED:
                case UNDELIV:
                case REJECTD:
                    ret = true;
                    SQLUtil.execute(() -> {
                        try (Connection conn = SQLUtil.getConnection()) {
                            boolean storeFaultyRecordIsOk = DAO.storeFaultyRecord(conn, smsCode, idGw, 0);
                            if (storeFaultyRecordIsOk) {
                                LOGGER.trace("Failure registered for message {}", smsCode);
                            } else {
                                LOGGER.warn("Unable to register failure for message {}", smsCode);
                            }
                        } catch (SQLException sqle) {
                            LOGGER.error("Unexpected DB exception", sqle);
                        }
                    });
                    break;
                case DELIVRD:
                    ret = true;
                    SQLUtil.execute(() -> {
                        try (Connection conn = SQLUtil.getConnection()) {
                            final Optional<Date> doneDate = dlr.parseDoneDateField();
                            Date date;
                            if (doneDate.isPresent()) {
                                date = doneDate.get();
                            } else {
                                date = new Date();
                            }
                            boolean storeSuccessRecordIsOk = DAO.storeSuccessRecord(conn, smsCode, idGw, 0, date);
                            if (storeSuccessRecordIsOk) {
                                LOGGER.trace("Message {} marked as delivered", smsCode);
                            } else {
                                LOGGER.warn("Unable to mark message {} as delivered", smsCode);
                            }
                        } catch (SQLException sqle) {
                            LOGGER.error("Unexpected DB exception", sqle);
                        } catch (ParseException pe) {
                            LOGGER.error(String.format("Unexpected parse exception while processing %s", dlr), pe);
                        }
                    });
                    break;
                default:
                    LOGGER.warn("Unexpected stat {} for {}", dlr, smsCode);
                    break;
            }
        }
        return ret;
    }

    @Override
    public void handleError(SubmitContext submitContext, Message message) {
        super.handleError(submitContext, message);
        try (Connection conn = (Connection) submitContext.getWrappedObject()) {
            boolean isOk = DAO.storeFaultyRecord(conn, message.getId().getPrimary(), message.getId().getSecondary(), 0);
            assert isOk;
        } catch (SQLException sqle) {
            LOGGER.error("Unexpected DB exception", sqle);
        }
    }
}
