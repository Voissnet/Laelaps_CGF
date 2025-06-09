package net.redvoiss.sms.smpp;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smpp.Data;
import org.smpp.ServerPDUEvent;
import org.smpp.ServerPDUEventListener;
import org.smpp.Session;
import org.smpp.SmppObject;
import org.smpp.WrongSessionStateException;
import org.smpp.pdu.DeliverSM;
import org.smpp.pdu.PDU;
import org.smpp.pdu.Request;
import org.smpp.pdu.Response;
import org.smpp.pdu.SubmitSMResp;
import org.smpp.pdu.ValueNotSetException;

import net.redvoiss.sms.smpp.cache.SelfExpiringCacheEsmeEventHandler;

/**
 *
 * @author Jorge Avila
 */
public class EsmeServerPDUEventListener extends SmppObject implements ServerPDUEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsmeServerPDUEventListener.class);
    private final Session session;

    /**
     * Cached handler
     */
    protected final SelfExpiringCacheEsmeEventHandler selfExpiringCacheEsmeEventHandler;

    /**
     * Constructor
     *
     * @param session
     * @param handler
     */
    public EsmeServerPDUEventListener(Session session, SelfExpiringCacheEsmeEventHandler handler) {
        this.session = session;
        this.selfExpiringCacheEsmeEventHandler = handler;
    }

    /**
     * Handles SMPP event
     *
     * @param spdue
     */
    @Override
    public void handleEvent(ServerPDUEvent spdue) {
        PDU pdu = spdue.getPDU();
        if (pdu == null) {
            LOGGER.warn("Empty PDU received");
        } else {
            if (pdu.isRequest()) {
                LOGGER.trace("receiver listener: handling request {}", pdu.debugString());
                Request request = (Request) pdu;
                try {
                    switch (pdu.getCommandId()) {
                        case Data.UNBIND:
                            try {
                                final Response response = request.getResponse();
                                session.respond(response);
                                session.close();
                            } catch (IOException | ValueNotSetException | WrongSessionStateException ex) {
                                LOGGER.error("Unexpected exception while replying unbind", ex);
                            }
                            break;
                        case Data.DELIVER_SM:
                            DeliverSM deliverSM = (DeliverSM) request;
                            LOGGER.info("SMPP ~ {}", deliverSM.debugString());
                            try {
                                selfExpiringCacheEsmeEventHandler.handleDLR(deliverSM.getReceiptedMessageId(), deliverSM.getShortMessage());
                            } catch (ValueNotSetException vnse) {
                                LOGGER.debug("Message id is missing from header", vnse);
                                selfExpiringCacheEsmeEventHandler.handleDLR(deliverSM.getShortMessage());
                            }
                            break;
                        default:
                            break;
                    }
                } finally {
                    try {
                        if (session.isOpened() && session.isBound()) {
                            final Response response = request.getResponse();
                            session.respond(response);
                        } else {
                            LOGGER.warn("Unable to send response due to session state is open:{} and bound:{}", session.isOpened(), session.isBound());
                        }
                    } catch (IOException | ValueNotSetException | WrongSessionStateException ex) {
                        LOGGER.error("Unexpected exception while replying request", ex);
                    }
                }
            } else if (pdu.isResponse()) {
                switch (pdu.getCommandId()) {
                    case Data.SUBMIT_SM_RESP:
                        SubmitSMResp submitSMResp = (SubmitSMResp) pdu;
                        LOGGER.info("SMPP ~ {}", submitSMResp.debugString());
                        selfExpiringCacheEsmeEventHandler.handleSubmitResponse(submitSMResp.getSequenceNumber(), submitSMResp.getCommandStatus(), submitSMResp.getMessageId());
                        break;
                    default:
                        break;
                }
            } else {
                LOGGER.debug("receiver listener: handling strange pdu {}", pdu.debugString());
            }
        }
    }

}
