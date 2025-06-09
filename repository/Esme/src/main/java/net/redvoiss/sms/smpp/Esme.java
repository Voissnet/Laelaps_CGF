package net.redvoiss.sms.smpp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jorge Avila
 */
public interface Esme {

    /**
     * Logger
     */
    static final Logger LOGGER = LoggerFactory.getLogger(Esme.class);
    /**
     * Stores windowing size
     */
    static final int WINDOWING_SIZE = Integer.parseInt(System.getProperty("esme.windowing", "0"));

    /**
     * Specifies configuration file name
     */
    static final String ESME_PROPERTIES = System.getProperty("esme.cfg", "esme.properties");

    /**
     * Defines how many messages should be send by second
     */
    static final int THROUGHPUT = Integer.parseInt(System.getProperty("esme.s.throughput", "10"));

    /**
     * Defines Enquire Link periodicity in seconds
     */
    static final int ENQUIRE_LINK_PERIODICITY = Integer.parseInt(System.getProperty("esme.el.periodicity", "90"));

    /**
     * Controls whether windowing setting should be enforced
     */
    static final boolean FLOW_CONTROL_ENABLED = false;

    /**
     *
     */
    static final String SMSC_IP = System.getProperty("smsc.ip", "smsc.ip");

    /**
     *
     */
    static final String SMSC_PORT = System.getProperty("smsc.port", "smsc.port");

    /**
     * Identifies the ESME system requesting to bind as a transmitter with the
     * SMSC.
     */
    static final String SYSTEM_ID = System.getProperty("esme.system_id", "esme.system_id");

    /**
     * The password may be used by the SMSC to authenticate the ESME requesting
     * to bind.
     */
    static final String PASSWORD = System.getProperty("esme.password", "esme.password");

    /**
     * Stores information from CVS repository
     */
    static final String REPO_INFO = System.getProperty("repo.info", "Esme.git");

    /**
     * Shows release info
     */
    static void showReleaseDetails() {
        String PROPERTY_FILE_LOCATION = Esme.REPO_INFO;
        String[] keys = new String[]{"git.commit.id.abbrev", "git.branch", "git.dirty"};
        StringBuilder sb = new StringBuilder();
        try {
            java.util.Enumeration<java.net.URL> urls = AbstractEsme.class.getClassLoader()
                    .getResources(PROPERTY_FILE_LOCATION);
            while (urls.hasMoreElements()) {
                java.net.URL url = urls.nextElement();
                try (java.io.InputStream manifestStream = url.openStream()) {
                    java.util.Properties p = new java.util.Properties();
                    p.load(manifestStream);
                    for (String key : keys) {
                        if (p.containsKey(key)) {
                            sb.append(" - ").append(key).append(":").append(p.getProperty(key));
                        }
                    }
                    LOGGER.info("Application runs using: {}", sb.toString());
                }
            }
        } catch (java.io.IOException ioe) {
            LOGGER.error(String.format("Unable to find {%s}", PROPERTY_FILE_LOCATION), ioe);
        }
    }
}
