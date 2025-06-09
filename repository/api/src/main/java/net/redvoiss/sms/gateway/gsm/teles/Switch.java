package net.redvoiss.sms.gateway.gsm.teles;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Level.SEVERE;

import net.redvoiss.sms.SMSException;
import net.redvoiss.sms.bean.NewMessage;
import net.redvoiss.sms.bean.PendingMessage;
import net.redvoiss.sms.bean.Slot;
import net.redvoiss.sms.dao.DAO;

public class Switch implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Switch.class.getName());
    private static int SLEEP_SECONDS = 60;
    private boolean m_keepRunning = true;
    private volatile List<Slot> m_availableSlotList;
    private final Semaphore m_spanSemaphore = new Semaphore(10, true);

    private GAO m_teles;
    private DAO m_dao;
    private int m_route;

    public static void main(String... args) throws SQLException, java.net.MalformedURLException {
        String gwHostname = args[0];
        String hostname = args[1];
        String username = args[2];
        String password = args[3];
        int route = Integer.parseInt(args[4]);
        new net.redvoiss.sms.gateway.gsm.teles.Switch(DAO.getDAO(), new GatewayImpl(gwHostname, hostname, username, password.toCharArray()), route).run();
    }

    public Switch(DAO dao, GAO gao, int route) throws SQLException {
        m_dao = dao;
        m_teles = gao;
        m_route = route;
        m_availableSlotList = m_dao.getAvailableSlot(m_route);
    }

    public void run() {
        Thread sendThread = new Thread() {
            public void run() {
                while (m_keepRunning) {
                    try {
                        final int availableSlotListSize = getAvailableSlotListSize();
                        if (availableSlotListSize == 0) {
                            LOGGER.warning("No slots configured for this gateway");
                        } else {
                            List<NewMessage> newMessageList = m_dao.getSMSList(m_route, availableSlotListSize);
                            if (newMessageList.isEmpty()) {
                                LOGGER.info("No messages to process");
                            } else {
                                for (int i = 0; i < newMessageList.size(); i++) {
                                    NewMessage nm = newMessageList.get(i);
                                    try {
                                        Slot slot = m_availableSlotList.get(i % availableSlotListSize);
                                        m_spanSemaphore.acquire();
                                        m_teles.sendMessage(slot.getSlotId(), nm.getDestination(), nm.getMessage());
                                        final String imsi = slot.getImsi();
                                        final String gsmId = imsi + nm.getDestination().getTarget();
                                        final String smsCode = nm.getSmsCode();
                                        if (!m_dao.markRecordReadyToBeProcessed(smsCode, gsmId, imsi)) {
                                            LOGGER.log(WARNING, "net.redvoiss.sms.teles.processing_message.mark_failed", new Object[]{smsCode, gsmId, m_route});
                                        }
                                    } catch (SMSException smse) {
                                        LOGGER.log(SEVERE, "Unexpected gw exception", smse);
                                    }
                                }
                            }
                        }
                        LOGGER.fine(String.format("Going to sleep for %d seconds", SLEEP_SECONDS));
                        Thread.sleep(SLEEP_SECONDS * 1000L);
                    } catch (SQLException sqle) {
                        LOGGER.log(SEVERE, "Unexpected DB exception", sqle);
                    } catch (InterruptedException ie) {
                        LOGGER.log(INFO, "Interrupted while sleeping...", ie);
                        m_keepRunning = false;
                    }
                }
                LOGGER.info("Shutting down.");
            }
        };

        Thread checkThread = new Thread() {
            public void run() {
                while (m_keepRunning) {
                    try {
                        List<PendingMessage> pendingSMSList = m_dao.getPendingSMSList(m_route);
                        Collections.sort(pendingSMSList, new java.util.Comparator<PendingMessage>() {
                            @Override
                            public int compare(PendingMessage pm1, PendingMessage pm2) {
                                if (pm1 == null && pm2 == null) {
                                    return 0;
                                }
                                if (pm1 == null) {
                                    return 1;
                                }
                                if (pm2 == null) {
                                    return -1;
                                }
                                Date d1 = pm1.getDate();
                                Date d2 = pm2.getDate();
                                if (d1 == null && d2 == null) {
                                    return 0;
                                }
                                if (d1 == null) {
                                    return 1;
                                }
                                if (d2 == null) {
                                    return -1;
                                }
                                return d2.compareTo(d1);
                            }
                        });
                        LOGGER.fine("Pending, reversed in time, messages are: " + pendingSMSList.toString());
                        for (Receipt r : m_teles.receiveReceiptList()) {
                            String imsi = r.getImsi();
                            String sender = r.getSender();
                            final String gsmId = imsi + "56" + sender;
                            PendingMessage pm = getPendingMessage(gsmId, pendingSMSList);
                            if (pm == null) {
                                LOGGER.warning(String.format("No pending message found matching gsm id (CODE_SMSC) {%s} in route {%d}", gsmId, m_route));
                            } else {
                                Date date = r.getDate();
                                switch (r.getReceiptType()) {
                                    case SENT:
                                        m_spanSemaphore.release();
                                        boolean success = m_dao.sent(gsmId, pm.getSmsCode(), date);
                                        assert success;
                                        break;
                                    case CONFIRMED:
                                        if (pm.getSmscGWId() == null) {
                                            m_dao.storeSuccessRecord(pm.getSmsCode(), getChannel(imsi), date);
                                        } else {
                                            m_dao.storeSuccessRecord(pm.getSmsCode(), getChannel(imsi), pm.getSmscGWId(), date);
                                        }
                                        break;
                                    case FAILED:
                                        if (pm.getSmscGWId() == null) {
                                            m_dao.storeFaultyRecord(pm.getSmsCode(), getChannel(imsi));
                                        } else {
                                            m_dao.storeFaultyRecord(pm.getSmsCode(), pm.getSmscGWId(), getChannel(imsi));
                                        }
                                        break;
                                    case UNCONFIRMED:
                                        if (pm.getSmscGWId() == null) {
                                            m_dao.storeAbandonedRecord(pm.getSmsCode(), getChannel(imsi), date);
                                        } else {
                                            m_dao.storeAbandonedRecord(pm.getSmsCode(), getChannel(imsi), pm.getSmscGWId(), date);
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                        LOGGER.fine(String.format("Going to sleep for %d seconds", SLEEP_SECONDS));
                        Thread.sleep(SLEEP_SECONDS * 1000L);
                    } catch (SMSException smse) {
                        LOGGER.log(SEVERE, "Unexpected gw exception", smse);
                    } catch (SQLException sqle) {
                        LOGGER.log(SEVERE, "Unexpected DB exception", sqle);
                    } catch (InterruptedException ie) {
                        LOGGER.log(INFO, "Interrupted while sleeping...", ie);
                        m_keepRunning = false;
                    }
                }
            }
        };

        Thread checkAvailabilityThread = new Thread() {
            public void run() {
                while (m_keepRunning) {
                    try {
                        synchronized (m_availableSlotList) {
                            m_availableSlotList = m_dao.getAvailableSlot(m_route);
                        }
                        Thread.sleep(SLEEP_SECONDS * 1000L);
                    } catch (SQLException sqle) {
                        LOGGER.log(SEVERE, "Unexpected DB exception", sqle);
                    } catch (InterruptedException ie) {
                        LOGGER.log(INFO, "Interrupted while sleeping...", ie);
                        m_keepRunning = false;
                    }
                }
            }
        };

        sendThread.start();
        checkThread.start();

        try {
            checkThread.join();
            sendThread.join();
        } catch (InterruptedException ie) {
            LOGGER.log(WARNING, "Unexpected interruption", ie);
        }
    }

    protected int getChannel(String imsi) {
        synchronized (m_availableSlotList) {
            Optional<Slot> optional = m_availableSlotList.stream().filter(o -> o.getImsi().equals(imsi)).findFirst();
            Slot slot = optional.get();
            return slot.getSlotId();
        }
    }

    protected int getAvailableSlotListSize() {
        synchronized (m_availableSlotList) {
            return m_availableSlotList.size();
        }
    }

    protected PendingMessage getPendingMessage(String gsmId, List<PendingMessage> pendingSMSList) {
        Optional<PendingMessage> optional = pendingSMSList.stream().filter(o -> o.getGsmId().equals(gsmId)).findFirst();
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }
}
