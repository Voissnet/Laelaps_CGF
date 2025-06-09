package net.redvoiss.sms;

import javax.annotation.Resource;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.sql.DataSource;

import java.util.logging.Logger;

import net.redvoiss.sms.management.SMSC;
import net.redvoiss.sms.smpp.App;

@Singleton
@Startup
public class SMSCBean {

    private static final Logger LOGGER = Logger.getLogger(SMSCBean.class.getName());

    private App m_app;

    @Resource(lookup = "jdbc/DB")
    private DataSource m_ds;

    @Resource(lookup = "concurrent/smsSKD")
    ManagedScheduledExecutorService m_scheduler;

    private SMSC m_smsc;

    @PreDestroy
    public void cleanup() {
        if (m_app == null) {
            LOGGER.warning("No SMPP core available");
        } else {
            m_app.cleanup();
            m_app = null;
        }
        if (m_smsc == null) {
            LOGGER.warning("No management available");
        } else {
            m_smsc.unregisterMBean();
        }
    }

    @PostConstruct
    public void init() {
        if (m_ds == null) {
            LOGGER.severe("Datasource unavailable");
        } else if (m_scheduler == null) {
            LOGGER.severe("Scheduler unavailable");
        } else {
            LOGGER.info("Datasource and scheduler available");
            if (Boolean.parseBoolean(System.getenv("SMSC_DISABLED"))) {
                LOGGER.warning("SMSC components were disabled.");
            } else {
                m_app = new App(m_ds, m_scheduler);
                m_app.init();
                (m_smsc = new SMSC(m_app.getShortMessageStore(), m_app.getPduProcessorGroup(), m_app.getProcessingThread())).registerMBean();
            }
        }
    }
}
