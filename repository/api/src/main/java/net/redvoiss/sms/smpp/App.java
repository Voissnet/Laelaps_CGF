package net.redvoiss.sms.smpp;

import org.smpp.SmppObject;
import org.smpp.debug.Debug;
import org.smpp.debug.Event;
import org.smpp.debug.FileEvent;
import org.smpp.debug.LoggerDebug;
import org.smpp.smscsim.DeliveryInfoSender;
import org.smpp.smscsim.PDUProcessor;
import org.smpp.smscsim.PDUProcessorFactory;
import org.smpp.smscsim.PDUProcessorGroup;
import org.smpp.smscsim.ShortMessageStore;
import org.smpp.smscsim.SMSCListener;
import org.smpp.smscsim.SMSCListenerImpl;
import org.smpp.smscsim.util.Table;
import org.smpp.util.ProcessingThread;

import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import javax.sql.DataSource;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

public class App {

    private static final String MANIFEST_FILE_NAME = "META-INF/MANIFEST.MF";
    protected final static String LISTEN_PORT = System.getProperty("listen.port", "30165"); //LeeArgumento(System.getProperty("listen.port", "30165"));
    protected final static String USERS_FILE = System.getProperty("users.file", "users.txt");
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    private DeliveryInfoSender deliveryInfoSender;
    private PDUProcessorGroup pduProcessorGroup = new PDUProcessorGroup();
    private SMSCListener smscListener = new SMSCListenerImpl(Integer.parseInt(LISTEN_PORT),
            true /* run asynchronously */);
    private ShortMessageStore shortMessageStore;

    static {
        Debug d = new LoggerDebug("SMSC.Debug");
        d.activate();
        Event e = new FileEvent("/tmp/", "smpp.evt");
        SmppObject.setDebug(d);
        SmppObject.setEvent(e);
    }
/*
    public static String LeeArgumento(String valor0)
    {
        String valor = valor0;
        if (!"".equals(System.getProperty("listen.port")))
           valor = System.getProperty("listen.port");
        System.out.println("ARGUMENTO LISTEN.PORT="+valor);               
        return valor;
    }
    */
    public App(DataSource ds, ScheduledExecutorService scheduler) {
        String msg = String.format("Working Directory is {%s}",
                java.nio.file.Paths.get(".").toAbsolutePath().normalize().toString());
        LOGGER.fine(msg);
        final String release = getRelease();
        LOGGER.info(release);
        try {
            shortMessageStore = new ShortMessageDBStore(ds);
            deliveryInfoSender = new DBDeliveryInfoSender(ds);
            try {
                Table aTable = new Table(USERS_FILE);
                PDUProcessorFactory aPDUProcessorFactory = new DBSimulatorPDUProcessorFactory(
                        ds, scheduler, pduProcessorGroup, shortMessageStore, deliveryInfoSender, aTable);
                smscListener.setPDUProcessorFactory(aPDUProcessorFactory);
            } catch (java.io.FileNotFoundException fnfe) {
                LOGGER.log(SEVERE, "Missing configuration file", fnfe);
            } catch (java.io.IOException ioe) {
                LOGGER.log(SEVERE, "IO Exception", ioe);
            }
        } catch (java.sql.SQLException sqle) {
            LOGGER.log(SEVERE, "DB Exception", sqle);
        }
    }

    private static String getRelease() {
        String ret = "Unable to read release details";
        try {
            java.util.Enumeration<java.net.URL> urls = App.class.getClassLoader().getResources(MANIFEST_FILE_NAME);
            while (urls.hasMoreElements()) {
                java.net.URL url = urls.nextElement();
                try (java.io.InputStream manifestStream = url.openStream()) {
                    java.util.jar.Manifest manifest = new java.util.jar.Manifest(manifestStream);
                    java.util.jar.Attributes attrs = manifest.getMainAttributes();
                    String ib = attrs.getValue("Implementation-Build");
                    String ibb = attrs.getValue("Implementation-Build-Branch");
                    if (ib == null && ibb == null) {
                        LOGGER.finest(
                                () -> String.format("Unable to read release details from {%s}", url.toExternalForm()));
                        /*for ( java.util.Map.Entry<Object,Object> m: attrs.entrySet() ) {
							LOGGER.info( String.format("{%s}: {%s} -> {%s}", url.toExternalForm(), String.valueOf(m.getKey()), String.valueOf(m.getValue())));
						}/*/
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

    public void cleanup() {
        LOGGER.fine("Clean up was invoked.");
        if (deliveryInfoSender != null) {
            deliveryInfoSender.stop();
            deliveryInfoSender = null;
            LOGGER.info("Processing thread was stopped.");
        }
        if (smscListener != null) {
            try {
                smscListener.stop();
                smscListener = null;
                LOGGER.info("Listener was stopped.");
            } catch (java.io.IOException ioe) {
                LOGGER.log(SEVERE, "Unexpected IO exception", ioe);
            }
        }
        if (pduProcessorGroup == null || pduProcessorGroup.count() == 0) {
            LOGGER.warning("Empty PDU processor group.");
        } else {
            final int size = pduProcessorGroup.count();
            for (int i = 0; i < size; i++) {
                PDUProcessor pduProcessor = pduProcessorGroup.get(i);
                if (pduProcessor == null) {
                    LOGGER.log(WARNING, "PDU processor {0} is unavailable", i);
                } else {
                    LOGGER.log(FINE, "PDU processor {0} is about to exit", i);
                    pduProcessor.exit();
                }
            }
            pduProcessorGroup = null;
        }
    }

    public void init() {
        LOGGER.fine("Init was invoked.");
        if (deliveryInfoSender == null) {
            LOGGER.severe("Processing thread will not be started.");
        } else {
            deliveryInfoSender.start();
            LOGGER.info("Processing thread was started.");
        }
        if (smscListener == null) {
            LOGGER.severe("Listener will not be started.");
        } else {
            try {
                smscListener.start();
                LOGGER.info(String.format("Listener was started on port {%s}.", LISTEN_PORT));
            } catch (java.io.IOException ioe) {
                LOGGER.log(SEVERE, "Unexpected IO exception", ioe);
            }
        }
    }

    public ProcessingThread getProcessingThread() {
        return deliveryInfoSender;
    }

    public PDUProcessorGroup getPduProcessorGroup() {
        return pduProcessorGroup;
    }

    public ShortMessageStore getShortMessageStore() {
        return shortMessageStore;
    }
}
