package net.redvoiss.sms.smpp.db.impl;

import java.sql.Connection;
import java.sql.SQLException;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import java.util.stream.Stream;
import net.redvoiss.sms.dao.DAO;
import net.redvoiss.sms.smpp.AbstractSubmitTask;
import net.redvoiss.sms.smpp.Esme;
import net.redvoiss.sms.smpp.EsmeEventHandler;
import net.redvoiss.sms.smpp.SubmitContext;
import net.redvoiss.sms.smpp.bean.Message;
import net.redvoiss.sms.smpp.db.SQLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smpp.Session;

import static net.redvoiss.sms.smpp.db.AbstractDBEsme.ROUTE;

/**
 *
 * @author Jorge Avila
 */
public abstract class AbstractSubmitTaskImpl extends AbstractSubmitTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSubmitTaskImpl.class);

    /**
     * Constructor
     *
     * @param session
     * @param esmeEventHandler
     */
    protected AbstractSubmitTaskImpl(Session session, EsmeEventHandler esmeEventHandler) {
        super(session, esmeEventHandler);
    }

    @Override
    public Stream<Message> getMessageStream(SubmitContext context) {
        Stream<Message> ret = Stream.empty();
        Connection conn = (Connection) context.getWrappedObject();
        try {
            ret = DAO.getSMSList(conn, ROUTE, Esme.THROUGHPUT).stream().map((newMessage) -> new MessageImpl(newMessage));
        } catch (SQLException sqle) {
            LOGGER.error("Unexpected DB exception", sqle);
        }
        return ret;
    }

    @Override
    public void run() {
        synchronized (this) {
            final long startTime = System.nanoTime();
            try (Connection conn = SQLUtil.getConnection()) {
                execute((SubmitContext) () -> {
                    return conn;
                });
            } catch (SQLException sqle) {
                LOGGER.error("Unexpected DB exception", sqle);
            }
            final long elapsedTime = NANOSECONDS.toSeconds(System.nanoTime() - startTime);
            if (elapsedTime > 1) {
                LOGGER.warn("Thread is dragging by {0}[s]", elapsedTime);
            }
        }
    }
}
