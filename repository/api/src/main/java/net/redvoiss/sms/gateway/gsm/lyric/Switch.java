package net.redvoiss.sms.gateway.gsm.lyric;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.redvoiss.sms.SMSException;
import net.redvoiss.sms.bean.Destination;
import net.redvoiss.sms.bean.Message;
import net.redvoiss.sms.bean.NewMessage;
import net.redvoiss.sms.bean.PendingMessage;
import net.redvoiss.sms.dao.DAO;
import net.redvoiss.sms.dao.DAO.ImsiRecord;
import net.redvoiss.sms.gateway.gsm.lyric.ChannelStatusResult.ChannelStatusRecord;
import net.redvoiss.sms.gateway.gsm.lyric.GAO.DeliveryStatus;

import static net.redvoiss.sms.gateway.gsm.lyric.Result.ErrorCode;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Level.SEVERE;
import static net.redvoiss.sms.gateway.gsm.lyric.ChannelStatusResult.ChannelStatusRecord.StateCode;

public class Switch implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(Switch.class.getName(), Switch.class.getName());
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);
    private static final ExecutorService CONFIRMATION_POOL = Executors.newSingleThreadExecutor();
    private static final DelayQueue<DelayedConfirmation> CONFIRMATION_QUEUE = new DelayQueue<DelayedConfirmation>();
    private final ExecutorService m_pool;
    private static final int RETRY_ATTEMPS = 3;
    private static final int TIMEOUT_THRESHOLD = 7;
    private volatile int m_timeoutCount = 0;
    private Map<String, ImsiRecord> m_imsi2Number;

    private DAO m_dao;
    private GAO m_gao;
    private int m_route;
    private int m_capacity;
    private String m_gateway;

    public static void verifyImsiConfiguracion(DAO m_dao, GAO m_gao, Map<String, ImsiRecord> m_imsi2Number)
            throws SMSException {
        ChannelStatusResult aChannelStatusResult = m_gao.getChannelStatus();
        if (aChannelStatusResult.isSuccess()) {
            for (ChannelStatusRecord csr : aChannelStatusResult.getChannels()) {
                if (StateCode.Registered.equals(csr.getState())) {
                    if (m_imsi2Number.keySet().contains(csr.getImsi())) {
                        LOGGER.fine(() -> String.format("Imsi {%s} is present", csr.getImsi()));
                        ImsiRecord ir = m_imsi2Number.get(csr.getImsi());
                        if (csr.isSendEnabled() && ir.isEnabled()) {
                            if (csr.getId() != ir.getSlot()) {
                                LOGGER.severe(String.format(
                                        "Imsi {%s} configured in different slots: {%s}[Gateway] v/s {%s}[DB]",
                                        csr.getImsi(), csr.getId(), ir.getSlot()));
                                System.exit(-1);
                            }
                        } else if (!csr.isSendEnabled() && !ir.isEnabled()) {
                            LOGGER.warning(() -> String.format("Imsi {%s} is disabled", csr.getImsi()));
                        } else {
                            LOGGER.severe(
                                    String.format("Imsi {%s} send status is inconsistent: {%s}[Gateway] v/s {%s}[DB]",
                                            csr.getImsi(), csr.isSendEnabled(), ir.isEnabled()));
                            System.exit(-2);
                        }
                    } else {
                        LOGGER.severe(String.format("Imsi {%s} is missing from DB configuration which is {%s}",
                                csr.getImsi(), m_imsi2Number.keySet()));
                        System.exit(-3);
                    }
                } else {
                    LOGGER.warning(() -> String.format("Unchecked slot {%s}. Its state was {%s}", csr.getId(),
                            csr.getState()));
                }
            }
        }
    }

    public Switch(GAO gao, int route, int capacity, String gateway) throws SQLException, SMSException {
        m_dao = DAO.getDAO();
        m_gao = gao;
        m_route = route;
        m_capacity = capacity;
        m_gateway = gateway;
        m_imsi2Number = m_dao.getRealNumberFromIMSI(route);
        verifyImsiConfiguracion(m_dao, m_gao, m_imsi2Number);
        m_pool = Executors.newFixedThreadPool(capacity);
        m_dao.getPendingSMSList(m_route).stream()
                .forEach(pendingMessage -> CONFIRMATION_QUEUE.add(new DelayedConfirmation(pendingMessage)));
    }

    protected static String getRelease() {
        final String MANIFEST_FILE_NAME = "META-INF/MANIFEST.MF";
        String ret = "Unable to read release details";
        try {
            java.util.Enumeration<java.net.URL> urls = Switch.class.getClassLoader().getResources(MANIFEST_FILE_NAME);
            while (urls.hasMoreElements()) {
                java.net.URL url = urls.nextElement();
                try (java.io.InputStream manifestStream = url.openStream()) {
                    java.util.jar.Manifest manifest = new java.util.jar.Manifest(manifestStream);
                    java.util.jar.Attributes attrs = manifest.getMainAttributes();
                    String ib = attrs.getValue("Implementation-Build");
                    String ibb = attrs.getValue("Implementation-Build-Branch");
                    if (ib == null && ibb == null) {
                        LOGGER.fine(
                                () -> String.format("Unable to read release details from {%s}", url.toExternalForm()));
                    } else {
                        ret = String.format("Running using commit {%s} on branch {%s}", ib, ibb);
                        break;
                    }
                }
            }
        } catch (java.io.IOException ioe) {
            LOGGER.log(WARNING, String.format("Unable to find {%s}", MANIFEST_FILE_NAME), ioe);
        }
        return ret;
    }

    public static void main(String... args) throws SQLException, IOException, SMSException {
        LOGGER.config(getRelease());
        if (args.length == 1) {
            Properties p = new Properties();
            p.load(Files.newInputStream(FileSystems.getDefault().getPath(args[0]), StandardOpenOption.READ));
            final java.net.URL url = new java.net.URL(p.getProperty("switch.url"));
            final int route = Integer.parseInt(p.getProperty("switch.route"));
            final int capacity = Integer.parseInt(p.getProperty("switch.capacity"));
            final String gw = p.getProperty("switch.name");
            LOGGER.config(String.format("Using {%s} for route {%d} with capacity {%d}", url, route, capacity));
            switch (url.getProtocol()) {
            case "https":
                new Switch(new LyricNativeSSLCommunicationImpl(url.getHost(), url.getPort()), route, capacity, gw)
                        .run();
                break;
            case "http":
                if (url.getUserInfo() == null || url.getUserInfo().isEmpty()) {
                    GAO g = new LyricNativePlainCommunicationImpl(url.getHost(), url.getPort());
                    new Switch(g, route, capacity, gw).run();
                } else {
                    GAO g = new LyricNativePlainCommunicationImpl(url.getUserInfo(), url.getHost(), url.getPort());
                    new Switch(g, route, capacity, gw).run();
                }
                break;
            default:
                LOGGER.warning(String.format("Unexpected protocol {%s}, exiting", url.getProtocol()));
                break;
            }
        } else {
            LOGGER.severe("Missing properties file, exiting");
        }
    }

    public void run() {
        Future<?> confirmerFuture = CONFIRMATION_POOL.submit(new Confirmer());
        ScheduledFuture<?> twoWayScheduledFuture = SCHEDULER.scheduleWithFixedDelay(new TwoWay(), 1, 10,
                TimeUnit.SECONDS);
        ScheduledFuture<?> senderScheduledFuture = SCHEDULER.scheduleWithFixedDelay(new Sender(), 2, 5,
                TimeUnit.SECONDS);

        try {
            senderScheduledFuture.get();
        } catch (InterruptedException ie) {
            LOGGER.log(SEVERE, "Unexpected interruption", ie);
        } catch (ExecutionException ee) {
            LOGGER.log(SEVERE, "Unexpected execution exception", ee);
        }

        CONFIRMATION_POOL.shutdown();
        SCHEDULER.shutdown();
        m_pool.shutdown();

        twoWayScheduledFuture.cancel(false);
        confirmerFuture.cancel(false);
    }

    class Sender implements Runnable {
        public void run() {
            if (m_timeoutCount > TIMEOUT_THRESHOLD) {
                Thread.currentThread().interrupt();
            }
            try {
                m_dao.getSMSList(m_route, m_capacity).stream().map(newMessage -> m_pool.submit(new Loader(newMessage)))
                        .collect(Collectors.toList()).stream().map(f -> {
                            LoadedMessage ret = null;
                            try {
                                ret = f.get();
                            } catch (InterruptedException ie) {
                                LOGGER.log(SEVERE, "Unexpected interruption", ie);
                            } catch (ExecutionException ee) {
                                LOGGER.log(SEVERE, "Unexpected execution exception", ee);
                            }
                            return ret;
                        }).filter(Objects::nonNull).map(lm -> m_pool.submit(lm)).collect(Collectors.toList()).stream()
                        .forEach(f -> {
                            try {
                                f.get();
                            } catch (InterruptedException ie) {
                                LOGGER.log(SEVERE, "Unexpected interruption", ie);
                            } catch (ExecutionException ee) {
                                LOGGER.log(SEVERE, "Unexpected execution exception", ee);
                            }
                        });
            } catch (SQLException sqle) {
                LOGGER.log(SEVERE, "Unexpected DB exception", sqle);
            }
        }
    }

    class Loader implements Callable<LoadedMessage> {
        NewMessage newMessage;

        Loader(NewMessage newMessage) {
            this.newMessage = newMessage;
        }

        public LoadedMessage call() {
            LoadedMessage ret = null;
            try {
                final Destination destination = newMessage.getDestination();
                final String smsCode = newMessage.getSmsCode();
                final String idGW = newMessage.getSmscGWId();
                if (destination == null || !destination.isOK()) {
                    if (idGW == null) {
                        m_dao.cleanFaultyRecord(smsCode);
                    } else {
                        m_dao.cleanFaultyRecord(smsCode, idGW);
                    }
                    LOGGER.log(SEVERE, "net.redvoiss.sms.gateway.gsm.lyric.send_message.invalid_destination",
                            new Object[] { destination.getScrambledTarget(), smsCode });
                } else {
                    SendMessageResult sendMessageResult = m_gao.sendMessage(destination.getTarget(),
                            newMessage.getMessage());
                    if (sendMessageResult == null) {
                        LOGGER.log(SEVERE, "net.redvoiss.sms.gateway.gsm.lyric.send_message.no_result",
                                new Object[] { destination.getScrambledTarget() });
                    } else if (sendMessageResult.isSuccess()) {
                        final int messageId = sendMessageResult.getId();
                        if (messageId > 0) {
                            m_dao.enqueued(String.valueOf(messageId), smsCode);
                            ret = new LoadedMessage(newMessage, messageId);
                            LOGGER.log(INFO, "net.redvoiss.sms.gateway.gsm.lyric.send_message.processed_item",
                                    new Object[] { destination.getScrambledTarget() });
                        } else {
                            LOGGER.log(SEVERE, "net.redvoiss.sms.gateway.gsm.lyric.send_message.invalid_id",
                                    new Object[] { destination.getScrambledTarget() });
                        }
                    } else {
                        LOGGER.warning(String.format("Error while trying to send message {%s}",
                                sendMessageResult.getErrorCode().getDescription()));
                    }
                }
            } catch (LyricTimeoutException lte) {
                if (m_timeoutCount++ > TIMEOUT_THRESHOLD) {
                    LOGGER.log(SEVERE, "Surpassed timeout threshold", lte);
                    Thread.currentThread().interrupt();
                }
            } catch (SMSException smse) {
                LOGGER.log(SEVERE, "Unexpected exception", smse);
            } catch (SQLException sqle) {
                LOGGER.log(SEVERE, "Unexpected DB exception", sqle);
            }
            return ret;
        }
    }

    void deleteMessage(int id) throws SMSException {
        synchronized (m_gao) {
            Result r = m_gao.deleteMessage(id);
            if (r == null) {
                LOGGER.log(SEVERE, "net.redvoiss.sms.gateway.gsm.lyric.delete_message.delete_null",
                        new Object[] { id });
            } else if (r.isSuccess()) {
                LOGGER.log(FINE, "net.redvoiss.sms.gateway.gsm.lyric.delete_message.delete_ok", new Object[] { id });
            } else {
                LOGGER.log(SEVERE, "net.redvoiss.sms.gateway.gsm.lyric.delete_message.delete_failed",
                        new Object[] { id, r.getErrorCode() });
                throw new SMSException(r.getErrorCode().getDescription());
            }
        }
    }

    class DelayedConfirmation implements Delayed {
        int id, retry;
        String smsCode, idGW;
        long delay = System.nanoTime() + NANOSECONDS.convert(5L, SECONDS);

        public long getDelay(TimeUnit unit) {
            return unit.convert(delay - System.nanoTime(), NANOSECONDS);
        }

        public DelayedConfirmation increaseDelay() {
            delay = System.nanoTime() + NANOSECONDS.convert(5L * retry, SECONDS);
            return this;
        }

        public int compareTo(Delayed delayed) {
            if (delayed == this) {
                return 0;
            }

            if (delayed instanceof DelayedConfirmation) {
                return (int) Math.signum(delay - ((DelayedConfirmation) delayed).delay);
            }

            return (int) Math.signum(getDelay(NANOSECONDS) - delayed.getDelay(NANOSECONDS));
        }

        DelayedConfirmation(PendingMessage pm) {
            id = Integer.parseInt(pm.getGsmId());
            smsCode = pm.getSmsCode();
            retry = pm.getRetry();
            idGW = pm.getSmscGWId();
        }

        String getSmsCode() {
            return smsCode;
        }

        int getRetry() {
            return retry++;
        }

        int getId() {
            return id;
        }

        String getIdGW() {
            return idGW;
        }

        @Override
        public String toString() {
            return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this);
        }
    }

    class Confirmer implements Runnable {
        public void run() {
            try {
                while (true) {
                    try {
                        DelayedConfirmation dc = CONFIRMATION_QUEUE.take();
                        LOGGER.fine(() -> String.format("Trying to confirm message {%s}", dc));
                        int id = dc.getId();
                        String idGW = dc.getIdGW();
                        MessageStatusResult msr = m_gao.getMessageStatus(id);
                        String smsCode = dc.getSmsCode();
                        if (msr == null) {
                            LOGGER.severe("No information available for message id: " + id);
                        } else if (msr.isSuccess()) {
                            switch (msr.getReportStage()) {
                            case NONE:
                            case TEMPORARY:
                                if (dc.getRetry() > RETRY_ATTEMPS) {
                                    LOGGER.fine(() -> String.format("Retry threshold reached for {%s}, abandoning.",
                                            smsCode));
                                    final int channel = msr.getChannel();
                                    if (idGW == null) {
                                        m_dao.storeAbandonedRecord(smsCode, channel, new Date());
                                    } else {
                                        m_dao.storeAbandonedRecord(smsCode, channel, idGW, new Date());
                                    }
                                    deleteMessage(id);
                                } else {
                                    m_dao.increaseRetry(smsCode);
                                    CONFIRMATION_QUEUE.add(dc.increaseDelay());
                                }
                                break;
                            case FINAL: {
                                final int channel = msr.getChannel();
                                if (DeliveryStatus.DELIVERED.equals(msr.getDeliveryStatus())) {
                                    LOGGER.log(FINE, "net.redvoiss.sms.gateway.gsm.lyric.confirmed_message.marked",
                                            new Object[] { smsCode, id });
                                    if (msr.getDeliveryDate() == null) {
                                        LOGGER.warning("Empty delivery date for message {" + smsCode + "}, id {" + id
                                                + "}. Using local date as replacement.");
                                        if (idGW == null) {
                                            m_dao.storeSuccessRecord(smsCode, channel, new Date());
                                        } else {
                                            m_dao.storeSuccessRecord(smsCode, channel, idGW, new Date());
                                        }
                                    } else {
                                        if (idGW == null) {
                                            m_dao.storeSuccessRecord(smsCode, channel, msr.getDeliveryDate());
                                        } else {
                                            m_dao.storeSuccessRecord(smsCode, channel, idGW, msr.getDeliveryDate());
                                        }
                                    }
                                } else {
                                    LOGGER.log(WARNING,
                                            "net.redvoiss.sms.gateway.gsm.lyric.confirmed_message.unexpected_delivery_status",
                                            new Object[] { smsCode, id, msr.getDeliveryStatus().getDescription() });
                                    if (idGW == null) {
                                        m_dao.storeFaultyRecord(smsCode, channel);
                                    } else {
                                        m_dao.storeFaultyRecord(smsCode, idGW, channel);
                                    }
                                }
                                deleteMessage(id);
                            }
                                break;
                            default:
                                LOGGER.log(SEVERE, "net.redvoiss.sms.gateway.gsm.lyric.check_status.unexpected_status",
                                        new Object[] { msr.getMessageStatus(), msr.getReportStage(), id });
                                break;
                            }
                        } else {
                            ErrorCode ec = msr.getErrorCode();
                            switch (ec) {
                            case DatabaseProblemORIdNotFound:
                                m_dao.storeFaultyRecord(smsCode, idGW, 0);
                                break;
                            default:
                                break;
                            }
                            LOGGER.log(SEVERE, "net.redvoiss.sms.gateway.gsm.lyric.check_status.error",
                                    ec.getDescription());
                        }
                    } catch (LyricTimeoutException lte) {
                        if (m_timeoutCount++ > TIMEOUT_THRESHOLD) {
                            LOGGER.log(SEVERE, "Surpassed timeout threshold", lte);
                            Thread.currentThread().interrupt();
                        }
                    } catch (SMSException smse) {
                        LOGGER.log(SEVERE, "Unexpected SMS exception", smse);
                    } catch (SQLException sqle) {
                        LOGGER.log(SEVERE, "Unexpected DB exception", sqle);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(SEVERE, "Unexpected exception", e);
            }
        }
    }

    class TwoWay implements Runnable {
        public void run() {
            try {
                ReceiveMessageResult receiveMessageResult = m_gao.receiveMessage();
                if (receiveMessageResult == null) {
                    LOGGER.warning("Unexpected empty receive message result");
                } else if (receiveMessageResult.isSuccess()) {
                    for (ReceiveMessageRecord receiveMessageRecord : receiveMessageResult.getReceiveMessageRecord()) {
                        if (receiveMessageRecord == null) {
                            LOGGER.warning("Unexpected null object: " + receiveMessageRecord);
                        } else {
                            int msgid = receiveMessageRecord.getId();
                            Result result = m_gao.deleteMessage(msgid);
                            if (result == null) {
                                LOGGER.warning(
                                        String.format("Empty result while deleting message with id {%d}", msgid));
                            } else if (result.isSuccess()) {
                                Destination numOrigen = receiveMessageRecord.getNumOrig();
                                if (numOrigen == null) {
                                    LOGGER.severe("Source number is empty");
                                } else if (numOrigen.isOK()) {
                                    String imsi = receiveMessageRecord.getImsi();
                                    String dstNumber = m_imsi2Number.get(imsi).getNumber();
                                    if (dstNumber == null) {
                                        LOGGER.warning("Destination number is missing for imsi: " + imsi);
                                    } else {
                                        String smsMessage = receiveMessageRecord.getMessage();
                                        int channelRcv = receiveMessageRecord.getChannel();
                                        try {
                                            Date recvDate = receiveMessageRecord.getReceivedDate();
                                            LOGGER.info(String.format(
                                                    "Gateway{%s}, Message {%d}, Reception date {%s}, Source {%s}, Message {%s}, Channel {%d}, IMSI {%s}, Destination {%s}",
                                                    m_gateway, msgid, recvDate, numOrigen, smsMessage, channelRcv, imsi,
                                                    dstNumber));
                                            if (java.nio.charset.StandardCharsets.ISO_8859_1.newEncoder()
                                                    .canEncode(smsMessage)) {
                                                m_dao.storeReply(String.valueOf(msgid), m_gateway, m_route,
                                                        numOrigen.getTarget(), dstNumber, smsMessage, recvDate);
                                            } else {
                                                LOGGER.warning("Invalid message: " + smsMessage);
                                            }
                                        } catch (ParseException pe) {
                                            LOGGER.log(SEVERE, "Unexpected parse exception", pe);
                                        }
                                    }
                                } else {
                                    LOGGER.warning(String.format("Discarding invalid source {%s}",
                                            numOrigen.getScrambledTarget()));
                                }
                            } else {
                                LOGGER.warning(String.format("Error while deleting message was {%s}",
                                        String.valueOf(result.getErrorCode())));
                            }
                        }
                    }
                } else {
                    LOGGER.warning(String.format("Error while receiving message was {%s}",
                            String.valueOf(receiveMessageResult.getErrorCode())));
                }
            } catch (LyricTimeoutException lte) {
                if (m_timeoutCount++ > TIMEOUT_THRESHOLD) {
                    LOGGER.log(SEVERE, "Surpassed timeout threshold", lte);
                    Thread.currentThread().interrupt();
                }
            } catch (SMSException smse) {
                LOGGER.log(SEVERE, "Unexpected SMS exception", smse);
            } catch (SQLException sqle) {
                LOGGER.log(SEVERE, "Unexpected DB exception", sqle);
            }
        }
    }

    class LoadedMessage extends Message implements Runnable {
        int gsmId;

        LoadedMessage(Message m, int gsmId) {
            super(m);
            this.gsmId = gsmId;
        }

        int getGsmId() {
            return gsmId;
        }

        public void run() {
            int retry = 0;
            cycle: while (true) {
                try {
                    int messageId = Integer.valueOf(getGsmId());
                    String idGW = getSmscGWId();
                    String smsCode = getSmsCode();
                    MessageStatusResult msr = m_gao.getMessageStatus(messageId);
                    if (msr == null) {
                        LOGGER.severe("No information available for message id: " + messageId);
                    } else if (msr.isSuccess()) {
                        switch (msr.getMessageStatus()) {
                        case NEW:
                            LOGGER.log(FINE, "net.redvoiss.sms.gateway.gsm.lyric.message_enqueued",
                                    new Object[] { smsCode, messageId });
                            break;
                        case PROCESSING:
                            if (retry++ > 12) {
                                LOGGER.log(SEVERE, "net.redvoiss.sms.gateway.gsm.lyric.message_stuck",
                                        new Object[] { smsCode, messageId });
                                deleteMessage(messageId);
                                m_dao.markLost(smsCode);
                                break cycle;
                            } else {
                                LOGGER.log(FINE, "net.redvoiss.sms.gateway.gsm.lyric.message_processing",
                                        new Object[] { smsCode, messageId });
                            }
                            break;
                        case SENT:
                            boolean sent = m_dao.sent(String.valueOf(messageId), smsCode, msr.getSendDate());
                            LOGGER.log(FINE, "net.redvoiss.sms.gateway.gsm.lyric.sent_message.marked",
                                    new Object[] { smsCode, messageId, sent });
                            CONFIRMATION_QUEUE.add(new DelayedConfirmation(new PendingMessage(this, messageId)));
                            break cycle;
                        case FAILURE: {
                            final int channel = msr.getChannel();
                            LOGGER.log(SEVERE, "net.redvoiss.sms.gateway.gsm.lyric.failure",
                                    new Object[] { smsCode, channel, msr.getLastError() });
                            if (idGW == null) {
                                m_dao.storeFaultyRecord(smsCode, channel);
                            } else {
                                m_dao.storeFaultyRecord(smsCode, idGW, channel);
                            }
                            deleteMessage(messageId);
                            break cycle;
                        }
                        default:
                            LOGGER.log(SEVERE, "net.redvoiss.sms.gateway.gsm.lyric.check_status.unexpected_status",
                                    new Object[] { msr.getMessageStatus(), msr.getReportStage(), messageId });
                            break cycle;
                        }
                    } else {
                        ErrorCode ec = msr.getErrorCode();
                        switch (ec) {
                        case DatabaseProblemORIdNotFound:
                            m_dao.storeFaultyRecord(smsCode, idGW, 0);
                            break;
                        default:
                            break;
                        }
                        LOGGER.log(SEVERE, "net.redvoiss.sms.gateway.gsm.lyric.check_status.error",
                                ec.getDescription());
                        break cycle;
                    }
                } catch (LyricTimeoutException lte) {
                    if (m_timeoutCount++ > TIMEOUT_THRESHOLD) {
                        LOGGER.log(SEVERE, "Surpassed timeout threshold", lte);
                        Thread.currentThread().interrupt();
                    }
                } catch (SMSException smse) {
                    LOGGER.log(SEVERE, "Unexpected SMS exception", smse);
                } catch (SQLException sqle) {
                    LOGGER.log(SEVERE, "Unexpected DB exception", sqle);
                }

                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(2));
                } catch (InterruptedException ie) {
                    LOGGER.log(SEVERE, "Unexpected interruption", ie);
                    break cycle;
                }
            }
        }
    }
}