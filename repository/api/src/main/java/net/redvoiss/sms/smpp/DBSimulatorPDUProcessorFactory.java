package net.redvoiss.sms.smpp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import org.smpp.smscsim.DeliveryInfoSender;
import org.smpp.smscsim.PDUProcessor;
import org.smpp.smscsim.PDUProcessorFactory;
import org.smpp.smscsim.PDUProcessorGroup;
import org.smpp.smscsim.ShortMessageStore;
import org.smpp.smscsim.SimulatorPDUProcessor;
import org.smpp.smscsim.SMSCSession;
import org.smpp.smscsim.util.Table;

import javax.sql.DataSource;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;

public class DBSimulatorPDUProcessorFactory implements PDUProcessorFactory {

    private final PDUProcessorGroup procGroup;
    private final ShortMessageStore messageStore;
    private final DeliveryInfoSender deliveryInfoSender;
    private final Table users;
    private final DataSource ds;
    private final ScheduledExecutorService scheduler;

    private static final Logger LOGGER = Logger.getLogger(DBSimulatorPDUProcessorFactory.class.getName());

    public DBSimulatorPDUProcessorFactory(
            DataSource ds,
            ScheduledExecutorService scheduler,
            PDUProcessorGroup procGroup,
            ShortMessageStore messageStore,
            DeliveryInfoSender deliveryInfoSender,
            Table users) {
        this.ds = ds;
        this.scheduler = scheduler;
        this.procGroup = procGroup;
        this.messageStore = messageStore;
        this.deliveryInfoSender = deliveryInfoSender;
        this.users = users;
    }

    @Override
    public PDUProcessor createPDUProcessor(SMSCSession session) {
        SimulatorPDUProcessor simPDUProcessor = new DBSimulatorPDUProcessor(ds, scheduler, session, messageStore, users);
        simPDUProcessor.setDisplayInfo(LOGGER.isLoggable(SEVERE));// This allows us to see client's bindreq and client's enquireLink by default
        simPDUProcessor.setGroup(procGroup);
        simPDUProcessor.setDeliveryInfoSender(deliveryInfoSender);
        dumpSource(session, "[sys] new connection accepted");
        return simPDUProcessor;
    }

    protected static void dumpSource(SMSCSession session, String msg) {
        if (session == null) {
            LOGGER.severe("No session");
        } else {
            final String remoteAddress = session.getConnection() == null /* Just in case */ ? null : session.getConnection().getAddress();
            if (remoteAddress == null) {
                LOGGER.warning(msg);
            } else {
                try {
                    InetAddress ia = InetAddress.getByName(remoteAddress);
                    final String remoteAddressName = ia.getCanonicalHostName();
                    if (remoteAddressName == null) {
                        LOGGER.log(INFO, "{0} from {1}", new String[]{msg, remoteAddress});
                    } else {
                        LOGGER.log(INFO, "{0} from {1}", new String[]{msg, remoteAddressName});
                    }
                } catch (UnknownHostException uhe) {
                    LOGGER.log(SEVERE, "Unexpected exception while resolving " + remoteAddress, uhe);
                }
            }
        }
    }
}
