package net.redvoiss.sms.smpp;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smpp.Data;
import org.smpp.Session;
import org.smpp.SmppException;
import org.smpp.TCPIPConnection;
import org.smpp.WrongSessionStateException;
import org.smpp.pdu.BindRequest;
import org.smpp.pdu.BindResponse;
import org.smpp.pdu.BindTransciever;
import org.smpp.pdu.EnquireLinkResp;
import org.smpp.pdu.PDUException;
import org.smpp.pdu.UnbindResp;
import org.smpp.pdu.WrongLengthOfStringException;

import net.redvoiss.sms.smpp.cache.SelfExpiringCacheEsmeEventHandler;
import net.redvoiss.sms.smpp.cache.SelfExpiringCache;
import net.redvoiss.sms.smpp.impl.SelfExpiringCacheEsmeEventHandlerImpl;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import net.redvoiss.sms.smpp.bean.Message;
import net.redvoiss.sms.smpp.cache.AbstractSelfExpiringCacheFactory;

/**
 *
 * @author Jorge Avila
 */
public abstract class AbstractEsme implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEsme.class);
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(3);
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4);
    private final Future<?> submitFuture;

    /**
     * Shadowed SMPP session
     */
    private final Session session;

    /**
     * Thread that will be executed when process goes down.
     */
    private final UnbindingThread unbindingThread;

    /**
     * Runs default setting
     *
     * @param args
     * @throws SmppException
     * @throws IOException
     */
    public static void main(String args[]) throws SmppException, IOException {
        new AbstractEsme(new SelfExpiringCacheEsmeEventHandlerImpl(
                new AbstractSelfExpiringCacheFactory() {
            @Override
            public SelfExpiringCache<String, Message.MessageId> getMessageId2MessageIdSelfExpiringCache(String name) {
                return new SelfExpiringCache<>(name);
            }

            @Override
            public SelfExpiringCache<Integer, Message> getSequence2MessageSelfExpiringCache(String name) {
                return new SelfExpiringCache<>(name);
            }
        }, null),
                (Session aSession, EsmeEventHandler aEsmeEventHandler) -> null) {
        }.run();
    }

    @Override
    public void run() {
        try {
            try {
                unbindingThread.enquireLinkFuture.get();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Unexpected interruption while waiting for thread", ie);
            } catch (ExecutionException ee) {
                LOGGER.warn("Unexpected execution exception", ee);
            }
        } finally {
            Future<?> submitTaskFuture = getSubmitTask();
            if (submitTaskFuture == null) {
                LOGGER.warn("Submit task was unnavailable");
            } else if (submitTaskFuture.isDone()) {
                LOGGER.trace("Submit task is already done");
            } else if (submitTaskFuture.isCancelled()) {
                LOGGER.trace("Submit task is already cancelled");
            } else {
                boolean cancelSubmitTaskOk = submitTaskFuture.cancel(true);
                if (cancelSubmitTaskOk) {
                    LOGGER.debug("Submit task cancelled");
                } else {
                    LOGGER.warn("Unable to cancel submit task");
                }
            }
            cleanUp();
        }
    }

    Future<?> getSubmitTask() {
        return submitFuture;
    }

    /**
     * Shutdowns session as gracefully as possible.
     */
    public void cleanUp() {
        if (session == null) {
            LOGGER.warn("Session is unavailable");
        } else if (session.isOpened()) {
            if (unbindingThread == null) {
                LOGGER.warn("Missing unbinding thread");
            } else {
                boolean shutdownHookSuccessfullyRemoved = Runtime.getRuntime().removeShutdownHook(unbindingThread);
                if (shutdownHookSuccessfullyRemoved) {
                    try {
                        POOL.submit(unbindingThread).get(3, MINUTES);
                    } catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException ex) {
                        LOGGER.warn("Unexpected exception while waiting for unbind to complete", ex);
                    }
                } else {
                    LOGGER.warn("Unable to remove unbind thread hook");
                }
            }
        } else {
            LOGGER.warn("Session is already closed");
        }
        shutdown(POOL);
        shutdown(SCHEDULER);
    }

    /**
     * Stops executor service
     *
     * @param executorService
     */
    protected void shutdown(ExecutorService executorService) {
        executorService.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!executorService.awaitTermination(1, MINUTES)) {
                executorService.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(1, MINUTES)) {
                    LOGGER.error("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            LOGGER.warn("Unexpected interruption", ie);
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Creates application
     *
     * @param esmeEventHandler
     * @param submitTaskAbstractFactory
     * @throws SmppException
     * @throws IOException
     */
    public AbstractEsme(SelfExpiringCacheEsmeEventHandler esmeEventHandler, SubmitTaskFactory submitTaskAbstractFactory) throws SmppException, IOException {
        Esme.showReleaseDetails();
        final InputStream is = AbstractEsme.class.getClassLoader().getResourceAsStream(Esme.ESME_PROPERTIES);
        Properties properties = new Properties();
        properties.load(is);
        final String ipAddress = properties.getProperty(Esme.SMSC_IP);
        final int port = Integer.parseInt(properties.getProperty(Esme.SMSC_PORT));
        final String username = properties.getProperty(Esme.SYSTEM_ID);
        final char[] password = properties.getProperty(Esme.PASSWORD).toCharArray();
        LOGGER.info("About to establish initial session directed towards {}:{}", ipAddress, port);
        session = new Session(new TCPIPConnection(ipAddress, port));
        EsmeServerPDUEventListener esmeServerPDUEventListener = new EsmeServerPDUEventListener(session, esmeEventHandler);
        unbindingThread = bind(username, password, session, esmeServerPDUEventListener);
        final SubmitTask submitTask = submitTaskAbstractFactory.createSubmitTask(session, esmeEventHandler);
        if (submitTask == null) {
            submitFuture = null;
        } else {
            submitFuture = SCHEDULER.scheduleAtFixedRate(submitTask, 15, 1, SECONDS);
        }
    }

    private static class UnbindingThread extends Thread {

        private final Session session;
        private final Future<?> enquireLinkFuture;
        private final List<Future<?>> selfExpiringCacheFutureList;

        UnbindingThread(Session session, Future<?> enquireLinkFuture, List<Future<?>> selfExpiringCacheFutureList) {
            this.session = session;
            this.enquireLinkFuture = enquireLinkFuture;
            this.selfExpiringCacheFutureList = selfExpiringCacheFutureList;
        }

        @Override
        public void run() {
            LOGGER.debug("About to cancel caching mechanism");
            selfExpiringCacheFutureList.stream().map((f) -> f.cancel(true)).forEachOrdered((cancelOk) -> {
                assert cancelOk;
            });
            LOGGER.debug("About to unbind");
            if (enquireLinkFuture.isDone()) {
                LOGGER.trace("Enquire Link task is already done");
            } else if (enquireLinkFuture.isCancelled()) {
                LOGGER.trace("Enquire Link task is already cancelled");
            } else {
                boolean cancelledEnquireLink = enquireLinkFuture.cancel(true);
                if (cancelledEnquireLink) {
                    if (session == null) {
                        LOGGER.warn("Session is missing");
                    } else {
                        //stops reading asynchronously
                        session.getReceiver().setServerPDUEventListener(null);
                        try {
                            if (session.isOpened() && session.isBound()) {
                                UnbindResp response = session.unbind();
                                if (response.getCommandStatus() == Data.ESME_ROK) {
                                    LOGGER.debug("Unbind response {}", response.debugString());
                                } else {
                                    LOGGER.error("Unexpected unbind response {}", response.debugString());
                                }
                            } else {
                                LOGGER.warn("Session is unavailable");
                            }
                        } catch (org.smpp.TimeoutException | PDUException | IOException | WrongSessionStateException ex) {
                            LOGGER.warn("Unexpected exception", ex);
                        } finally {
                            if (session.isOpened()) {
                                try {
                                    session.close();
                                } catch (IOException | WrongSessionStateException ex) {
                                    LOGGER.warn("Unexpected exception while closing connection", ex);
                                }
                            }
                        }
                    }
                } else {
                    LOGGER.warn("Unable to cancel Enquire Link task");
                }
            }
        }
    };

    private static UnbindingThread bind(String user, char[] password, Session session, EsmeServerPDUEventListener esmeServerPDUEventListener) throws SmppException, IOException {
        UnbindingThread ret = null;
        BindRequest bindRequest = new BindTransciever();
        try {
            bindRequest.setSystemId(user);
            bindRequest.setPassword(new String(password));
        } catch (WrongLengthOfStringException wlose) {
            LOGGER.warn("Unexpected length of string", wlose);
            throw wlose;
        }
        bindRequest.setInterfaceVersion(Data.SMPP_V34);
        LOGGER.debug("Bind request {}", bindRequest.debugString());
        try {
            BindResponse bindResponse = session.bind(bindRequest, esmeServerPDUEventListener);
            if (bindResponse == null) {
                LOGGER.error("Unexpected empty binding response");
            } else {
                LOGGER.debug("Bind response {}", bindResponse.debugString());
                if (bindResponse.getCommandStatus() == Data.ESME_ROK) {
                    LOGGER.debug("Starting Enquire Link task");
                    Future<?> enquireLinkFuture = SCHEDULER.scheduleWithFixedDelay(() -> {
                        enquireLink(session);
                    }, 1, Esme.ENQUIRE_LINK_PERIODICITY, SECONDS);
                    ret = new UnbindingThread(session, enquireLinkFuture,
                            esmeServerPDUEventListener.selfExpiringCacheEsmeEventHandler.activate(POOL));
                    LOGGER.debug("Adding shutdown hook");
                    Runtime.getRuntime().addShutdownHook(ret);
                } else {
                    LOGGER.warn("Bind failed, code " + bindResponse.getCommandStatus());
                }
            }
        } catch (SmppException ex) {
            LOGGER.warn("Unexpected SMPP exception", ex);
            throw ex;
        } catch (IOException ioe) {
            LOGGER.warn("Unexpected IO exception", ioe);
            throw ioe;
        }
        return ret;
    }

    /**
     * Performs confidence check of the communication path between an ESME and
     * an SMSC
     *
     * @param session
     * @throws RuntimeException
     */
    private static void enquireLink(Session session) throws RuntimeException {
        try {
            EnquireLinkResp resp = session.enquireLink();
            if (resp == null) {
                LOGGER.debug("Enquire Link request sent");
            } else {
                LOGGER.warn("Unexpected result while Link request was sent: {}", resp.debugString());
            }
        } catch (IOException ioe) {
            if (session.isOpened()) {
                LOGGER.warn("Unexpected IO exception while enquiring link when session is still open", ioe);
            } else {
                LOGGER.warn("Unexpected IO exception while enquiring link when session is closed", ioe);
            }
            throw new RuntimeException(ioe);
        } catch (org.smpp.TimeoutException | PDUException | WrongSessionStateException ex) {
            LOGGER.error("Enquire Link operation failed", ex);
            throw new RuntimeException(ex);
        }
    }

}
