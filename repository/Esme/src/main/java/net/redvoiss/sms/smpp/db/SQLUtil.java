package net.redvoiss.sms.smpp.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.sql.DataSource;
import net.redvoiss.sms.dao.DAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jorge Avila
 * @see
 * https://stackoverflow.com/questions/1915992/proper-usage-of-jdbc-connection-pool-glassfish
 */
public class SQLUtil {

    private static DataSource dataSource;
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor();
    private static final Logger LOGGER = LoggerFactory.getLogger(SQLUtil.class);

    static {
        try {
            dataSource = DAO.buildOracleDataSource();
        } catch (SQLException sqle) {
            throw new ExceptionInInitializerError(sqle);
        }
    }

    /**
     * Creates connection
     *
     * @return new connection
     * @throws SQLException
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Gets reference
     *
     * @return
     */
    public static ExecutorService getExecutionPool() {
        return POOL;
    }

    /**
     * Blocks for the specified amount of time
     *
     * @param task
     */
    public static void execute(Runnable task) {
        Future future = null;
        try {
            long startTime = System.nanoTime();
            future = POOL.submit(task);
            future.get(3, TimeUnit.MINUTES);
            long duration = System.nanoTime() - startTime;
            LOGGER.debug("Task execution took {} [ms]", TimeUnit.NANOSECONDS.toMillis(duration));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Unexpected interruption", ex);
        } catch (ExecutionException ex) {
            LOGGER.error("Unexpected execution exception", ex);
        } catch (TimeoutException ex) {
            LOGGER.error("DB took too long to process", ex);
            if (future == null) {
                LOGGER.warn("Task is unnavailable");
            } else {
                boolean cancelOk = future.cancel(true);
                if (cancelOk) {
                    LOGGER.trace("Task cancelled");
                } else {
                    LOGGER.warn("Unable to cancel task");
                }
            }
        }
    }

    /**
     * Blocks for the specified amount of time
     *
     * @param task
     * @return
     */
    public static List execute(Callable<List> task) {
        List ret = Collections.emptyList();
        Future<List> future = null;
        try {
            long startTime = System.nanoTime();
            future = POOL.submit(task);
            ret = future.get(3, TimeUnit.MINUTES);
            long duration = System.nanoTime() - startTime;
            LOGGER.debug("Task execution took {} [ms]", TimeUnit.NANOSECONDS.toMillis(duration));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Unexpected interruption", ex);
        } catch (ExecutionException ex) {
            LOGGER.error("Unexpected execution exception", ex);
        } catch (TimeoutException ex) {
            LOGGER.error("DB took too long to process", ex);
            if (future == null) {
                LOGGER.warn("Task is unnavailable");
            } else {
                boolean cancelOk = future.cancel(true);
                if (cancelOk) {
                    LOGGER.trace("Task cancelled");
                } else {
                    LOGGER.warn("Unable to cancel task");
                }
            }
        }
        return ret;
    }
}
