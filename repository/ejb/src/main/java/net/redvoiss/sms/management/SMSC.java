package net.redvoiss.sms.management;

import java.util.logging.Logger;
import javax.management.ObjectName;
import javax.management.MBeanServer;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.InstanceNotFoundException;

import org.smpp.smscsim.ShortMessageStore;
import org.smpp.smscsim.PDUProcessorGroup;
import org.smpp.smscsim.SimulatorPDUProcessor;
import org.smpp.util.ProcessingThread;

import static java.util.logging.Level.SEVERE;

/**
 * Based on
 * http://www.adam-bien.com/roller/abien/entry/singleton_the_simplest_possible_jmx
 * http://insidecoffe.blogspot.cl/2011/05/configuration-parameter-injection-with.html
 * https://blogs.oracle.com/jmxetc/entry/how_to_retrieve_remote_jvm
 * https://docs.oracle.com/javase/tutorial/jmx/mbeans/standard.html
 */
public class SMSC implements SMSCMBean {

    protected static final Logger LOGGER = Logger.getLogger(SMSC.class.getName());
    private ObjectName m_objectName;
    private final MBeanServer m_mbeanServer;

    private final ShortMessageStore m_shortMessageStore;
    private final PDUProcessorGroup m_pduProcessorGroup;
    private final ProcessingThread m_processingThread;

    public SMSC(ShortMessageStore shortMessageStore, PDUProcessorGroup pduProcessorGroup, ProcessingThread processingThread) {
        m_mbeanServer = java.lang.management.ManagementFactory.getPlatformMBeanServer();
        m_shortMessageStore = shortMessageStore;
        m_pduProcessorGroup = pduProcessorGroup;
        m_processingThread = processingThread;
    }

    @Override
    public String printShortMessageStoreContents() {
        return m_shortMessageStore == null ? null : m_shortMessageStore.print();
    }

    @Override
    public void enablePDUProcessorDisplayInfo(int i) {
        if (m_pduProcessorGroup == null) {
            LOGGER.warning("Unavailable");
        } else {
            ((SimulatorPDUProcessor) m_pduProcessorGroup.get(i)).setDisplayInfo(true);
        }
    }

    public void startProcessingThread() {
        if (m_processingThread == null) {
            LOGGER.warning("Unavailable");
        } else {
            m_processingThread.start();
        }
    }

    @Override
    public void disablePDUProcessorDisplayInfo(int i) {
        if (m_pduProcessorGroup == null) {
            LOGGER.warning("Unavailable");
        } else {
            ((SimulatorPDUProcessor) m_pduProcessorGroup.get(i)).setDisplayInfo(false);
        }
    }

    @Override
    public void enableEventTrace() {
        org.smpp.SmppObject.getEvent().activate();
    }

    @Override
    public void disableEventTrace() {
        org.smpp.SmppObject.getEvent().deactivate();
    }

    public void registerMBean() {
        try {
            m_objectName = new ObjectName("net.redvoiss.sms:type=Monitoring");
            if (m_mbeanServer.isRegistered(m_objectName)) {
                unregisterMBean();
            }
            try {
                m_mbeanServer.registerMBean(this, m_objectName);
            } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
                LOGGER.log(SEVERE, "Unexpected exception", e);
            }
        } catch (MalformedObjectNameException e) {
            LOGGER.log(SEVERE, "Unexpected exception", e);
        }
    }

    public void unregisterMBean() {
        try {
            m_mbeanServer.unregisterMBean(m_objectName);
        } catch (InstanceNotFoundException | MBeanRegistrationException e) {
            LOGGER.log(SEVERE, "Unexpected exception", e);
        }
    }
}
