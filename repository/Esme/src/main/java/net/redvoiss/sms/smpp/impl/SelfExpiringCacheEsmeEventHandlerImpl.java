package net.redvoiss.sms.smpp.impl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import net.redvoiss.sms.smpp.SubmitContext;
import net.redvoiss.sms.smpp.cache.AbstractSelfExpiringCacheFactory;
import net.redvoiss.sms.smpp.cache.SelfExpiringCache;
import net.redvoiss.sms.smpp.cache.SelfExpiringCacheEsmeEventHandler;
import net.redvoiss.sms.smpp.dlr.AbstractDlrParserFactory;
import net.redvoiss.sms.smpp.dlr.DlrParser;
import net.redvoiss.sms.smpp.dlr.DlrParser.StatEnum;
import net.redvoiss.sms.smpp.bean.Message;
import net.redvoiss.sms.smpp.bean.Message.MessageId;
import net.redvoiss.sms.smpp.cache.SelfExpiringCacheException;
import net.redvoiss.sms.smpp.mbean.EsmeStatistic;
//import java.util.logging.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smpp.Data;

/**
 *
 * @author Jorge Avila
 */
public class SelfExpiringCacheEsmeEventHandlerImpl extends EsmeStatistic implements SelfExpiringCacheEsmeEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SelfExpiringCacheEsmeEventHandlerImpl.class);
    
    //static final Logger LOGGER = Logger.getLogger(SelfExpiringCacheEsmeEventHandlerImpl.class.getName(), SelfExpiringCacheEsmeEventHandlerImpl.class.getName());
    
    
    private final AbstractDlrParserFactory abstractDlrParserFactory;
    /**
     * Caching mechanism with expiration policy in case DLR never arrives to the
     * platform (which is very likely for some providers).
     */
    private final SelfExpiringCache<Integer, Message> sequence2Message;
    private final SelfExpiringCache<String, MessageId> messageId2MessageId;

    /**
     *
     * @param abstractSelfExpiringCacheFactory
     * @param abstractDlrParserFactory Factory that allows to create a new DLR
     */
    public SelfExpiringCacheEsmeEventHandlerImpl(AbstractSelfExpiringCacheFactory abstractSelfExpiringCacheFactory, AbstractDlrParserFactory abstractDlrParserFactory) {
        this.abstractDlrParserFactory = abstractDlrParserFactory;
        messageId2MessageId = abstractSelfExpiringCacheFactory.getMessageId2MessageIdSelfExpiringCache("message_id");
        sequence2Message = abstractSelfExpiringCacheFactory.getSequence2MessageSelfExpiringCache("sequence");
    }

    /**
     * Handles SMSC reply
     *
     * @param sequenceNumber sequence number
     * @param cStatus command status
     * @param messageId message id according to SMSC
     */
    @Override
    public void handleSubmitResponse(Integer sequenceNumber, int cStatus, String messageId) {
        if (sequence2Message.containsKey(sequenceNumber)) {//Avoids processing a sequence ocurred on a different connection
            try {
                final Message message = sequence2Message.remove(sequenceNumber);
                if (message == null) {
                    LOGGER.debug("Handling submit response: Searching message for sequence number is {}, command status is {}, message id is {}", sequenceNumber, cStatus, messageId);
                } else {
                    processSubmitResponse(cStatus, sequenceNumber, messageId, message.getId());
                }
            } catch (SelfExpiringCacheException sece) {
                LOGGER.warn(String.format("Handling submit response: Unable to remove element with key %s from cache", sequenceNumber), sece);
            }
        } else {
            LOGGER.warn("Handling submit response: there is no cache reference for submit request sequence {}", sequenceNumber);
        }
        incrementSubmitResponseCount();
    }

    /**
     *
     * @param cStatus
     * @param seqN
     * @param messageId
     * @param id
     */
    protected void processSubmitResponse(int cStatus, int seqN, String messageId, MessageId id) {
        if (Data.ESME_ROK == cStatus) {
            LOGGER.debug("Handling submit response: sequence number is {}, message id is {} and message was {}", seqN, messageId, id.getPrimary());
            try {
                Object o = messageId2MessageId.put(messageId, id);
                assert o == null;
            } catch (SelfExpiringCacheException sece) {
                LOGGER.warn(String.format("Unable to put element %s into cache using key %s", id, messageId), sece);
            }
        } else {
            LOGGER.debug("Handling submit error response: sequence number is {}, command status is {}, message id is {} and message was {}", seqN, cStatus, messageId, id.getPrimary());
        }
    }

    /**
     * Obtains message id from PDU header.
     *
     * @param messageId
     * @param dlr
     */
    @Override
    public void handleDLR(String messageId, String dlr) {
        LOGGER.debug("Handling delivery: DLR to be processed is {}", dlr);
        final MessageId id = messageId2MessageId.get(messageId);
        if (id == null) {
            LOGGER.warn("Handling delivery: message id {} is missing from cache", messageId);
        } else {
            String smsCode = id.getPrimary();
            if (smsCode == null) {
                LOGGER.warn("Handling delivery: message is missing for message id {} and DLR message {}", messageId, dlr);
            } else {
                LOGGER.debug("Handling delivery: message code is {} for message id {} and DLR message {}", smsCode, messageId, dlr);
                if (abstractDlrParserFactory == null) {
                    LOGGER.warn("DLR parser factory is unnavailable");
                } else {
                    final boolean permanent = processDLR(id, abstractDlrParserFactory.createDlrParser(dlr));
                    if (permanent) {
                        try {
                            Object o = messageId2MessageId.remove(messageId);
                            if (o == null) {
                                LOGGER.warn("Handling delivery: Missinge reference associtated to {}", messageId);
                            } else {
                                LOGGER.trace("Handling delivery: Processing {} was handled definitively by removing {}", messageId, o);
                            }
                        } catch (SelfExpiringCacheException sece) {
                            LOGGER.warn(String.format("Handling delivery: Unable to remove element with key %s from cache", messageId), sece);
                        }
                    } else {
                        LOGGER.trace("Handling delivery: Processing {} was handled temporarily", messageId);
                    }
                }
            }
        }
        incrementDLRCount();
    }

    /**
     * Returns true if state processed is considered permanent, e.g. DELIVRD.
     *
     * @param messageId
     * @param dlr
     * @return
     */
    protected boolean processDLR(MessageId messageId, DlrParser dlr) {
        boolean ret = false;
        String smsCode = messageId.getPrimary(), idGw = messageId.getSecondary();
        LOGGER.debug("Processing DLR for {}/{}", smsCode, idGw);
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
                    break;
                case DELIVRD:
                    ret = true;
                    try {
                        final Optional<Date> doneDate = dlr.parseDoneDateField();
                        if (doneDate.isPresent()) {
                            Date date = doneDate.get();
                            LOGGER.debug("Delivered date is {}", date);
                        } else {
                            LOGGER.warn("Delivered date is missing from {}", String.valueOf(dlr));
                        }
                    } catch (ParseException pe) {
                        LOGGER.error(String.format("Unexpected parse exception while processing %s", dlr), pe);
                    }
                    break;
                default:
                    LOGGER.warn("Unexpected stat {} for {}", dlr, smsCode);
                    break;
            }
        }
        return ret;
    }

    /**
     * Extracts message id from SMSC reply
     *
     * @param dlr message from SMSC
     */
    @Override
    public void handleDLR(String dlr) {
        LOGGER.trace("Handling delivery: DLR message is {}", dlr);
        if (abstractDlrParserFactory == null) {
            LOGGER.warn("DLR parser is unnavailable");
        } else {
            final Optional<String> id = abstractDlrParserFactory.createDlrParser(dlr).parseIdField();
            if (id.isPresent()) {
                handleDLR(id.get(), dlr);
            }
        }
        incrementDLRCount();
    }

    /**
     * Maps sequence number against message that originated the flow
     *
     * @param sequenceNumber
     * @param message
     */
    @Override
    public void handleSubmit(SubmitContext submitContext, Integer sequenceNumber, Message message) {
        LOGGER.trace("Handling submit: sequence number is {} and message is {}", sequenceNumber, message);
        try {
            Object o = sequence2Message.put(submitContext, sequenceNumber, message);
            if (o == null) {
                incrementSubmitCount();
            } else {
                LOGGER.warn("Element {} already present for {}", o, sequenceNumber, o);
            }
        } catch (SelfExpiringCacheException sece) {
            LOGGER.warn(String.format("Unable to put element %s into cache using key %s", message, sequenceNumber), sece);
        }
    }

    @Override
    public void handleError(SubmitContext submitContext, Message message) {
        LOGGER.trace("Handling submit error: message is {}", message);
        incrementErrorCount();
    }

    @Override
    public List<Future<?>> activate(ExecutorService pool) {
        List<Future<?>> ret = new ArrayList<>();
        LOGGER.debug("Activates self expiring cache objects");
        ret.add(sequence2Message.activateMonitoringTask(pool));
        ret.add(messageId2MessageId.activateMonitoringTask(pool));
        return ret;
    }
}
