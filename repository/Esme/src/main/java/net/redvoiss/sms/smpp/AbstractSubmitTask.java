package net.redvoiss.sms.smpp;

import net.redvoiss.sms.smpp.bean.Message;
import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smpp.Data;
import org.smpp.Session;
import org.smpp.WrongSessionStateException;
import org.smpp.pdu.PDUException;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.SubmitSMResp;
import org.smpp.pdu.WrongLengthOfStringException;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 *
 * @author Jorge Avila
 */
public abstract class AbstractSubmitTask implements SubmitTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSubmitTask.class);
    private final Session session;
    private final EsmeEventHandler esmeEventHandler;

    /**
     * Constructor
     *
     * @param session
     * @param esmeEventHandler
     */
    protected AbstractSubmitTask(Session session, EsmeEventHandler esmeEventHandler) {
        this.session = session;
        this.esmeEventHandler = esmeEventHandler;
    }

    @Override
    public abstract Stream<Message> getMessageStream(SubmitContext submitContext);

    /**
     * Allows this method to be overridden by a subclass
     *
     * @param message
     * @return
     * @throws WrongLengthOfStringException
     * @throws java.io.UnsupportedEncodingException
     */
    SubmitSM produceSubmitSM(Message message) throws WrongLengthOfStringException, java.io.UnsupportedEncodingException {
        SubmitSM ret = new SubmitSM();
        ret.assignSequenceNumber();
        ret.setRegisteredDelivery(Data.SM_SMSC_RECEIPT_REQUESTED);
        ret.setSourceAddr(Data.GSM_TON_INTERNATIONAL, Data.GSM_NPI_E164, message.getSourceAddress());
        ret.setDestAddr(Data.GSM_TON_INTERNATIONAL, Data.GSM_NPI_ISDN, message.getDestinationAddress());
        ret.setDataCoding((byte) 3);
        ret.setShortMessage(message.getContent(), message.getEncoding());
        return ret;
    }

    @Override
    public abstract Predicate<Message> getFilter();

    @Override
    public void run() {
        synchronized (this) {
            final long startTime = System.nanoTime();
            execute((SubmitContext) () -> {
                throw new UnsupportedOperationException("Not supported.");
            });
            final long elapsedTime = NANOSECONDS.toSeconds(System.nanoTime() - startTime);
            if (elapsedTime > 1) {
                LOGGER.warn("Thread is dragging by {0}[s]", elapsedTime);
            }
        }
    }

    /**
     * Executes message submission
     *
     * @param submitContext
     */
    protected void execute(SubmitContext submitContext) {
        getMessageStream(submitContext).
                filter(getFilter()::test).
                map((message) -> {
                    LOGGER.trace("About to process message {}", message);
                    TransmittedMessage ret = null;
                    try {
                        SubmitSM request = produceSubmitSM(message);
                        synchronized (session) {
                            SubmitSMResp resp = session.submit(request);
                            assert resp == null;
                        }
                        LOGGER.info("SMPP ~ {} ~ {}", message.getId().getPrimary(), request.debugString());
                        ret = new TransmittedMessageImpl(request, message);
                    } catch (IOException ioe) {
                        LOGGER.error(String.format("Unable to send message %s", message.getId()), ioe);
                        /* Stops processing */
                        throw new RuntimeException(ioe);
                    } catch (WrongLengthOfStringException wlose) {
                        LOGGER.warn("Unexpected length of string", wlose);
                        esmeEventHandler.handleError(submitContext, message);
                    } catch (org.smpp.TimeoutException te) {
                        LOGGER.warn("Unexpected time out", te);
                    } catch (PDUException pdue) {
                        LOGGER.warn("Unexpected PDU", pdue);
                        esmeEventHandler.handleError(submitContext, message);
                    } catch (WrongSessionStateException wsse) {
                        LOGGER.warn("Unexpected session state", wsse);
                    }
                    return ret;
                }).
                filter(x -> x != null).
                forEach((t) -> esmeEventHandler.handleSubmit(submitContext, t.getSequence(), t.getMessage()));
    }

    private interface TransmittedMessage {

        Integer getSequence();

        Message getMessage();
    }

    private static class TransmittedMessageImpl implements TransmittedMessage {

        private final SubmitSM request;
        private final Message message;

        public TransmittedMessageImpl(SubmitSM request, Message message) {
            this.request = request;
            this.message = message;
        }

        @Override
        public Integer getSequence() {
            return request.getSequenceNumber();
        }

        @Override
        public Message getMessage() {
            return message;
        }
    }
}
