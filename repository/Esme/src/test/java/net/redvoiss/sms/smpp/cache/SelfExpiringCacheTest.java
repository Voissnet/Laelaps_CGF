package net.redvoiss.sms.smpp.cache;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * @author Jorge Avila
 */
public class SelfExpiringCacheTest {

    private ExecutorService pool;

    /**
     * Prepares test
     */
    @BeforeEach
    public void prepare() {
        pool = Executors.newSingleThreadExecutor();
    }

    /**
     * Tests shutdown
     *
     * @throws InterruptedException
     */
    @Test
    public void testPoolShutdownNow() throws InterruptedException {
        SelfExpiringCache<Integer, String> test = new SelfExpiringCache<>("test1");
        Future<?> monitoringTaskFuture = test.activateMonitoringTask(pool);
        assertFalse(test.containsKey(1));
        assertTrue(monitoringTaskFuture.cancel(true));
        List<Runnable> runnableList = pool.shutdownNow();
        assertNotNull(runnableList);
        assertTrue(runnableList.isEmpty());
        pool.awaitTermination(1, TimeUnit.MINUTES);
        assertTrue(pool.isTerminated());
    }

    /**
     * Test cancellation
     *
     * @throws InterruptedException
     */
    @Test
    public void testCancellation() throws InterruptedException {
        SelfExpiringCache<Integer, String> test = new SelfExpiringCache<>("test2");
        Future<?> monitoringTaskFuture = test.activateMonitoringTask(pool);
        assertFalse(test.containsKey(1));
        pool.shutdown();
        assertTrue(monitoringTaskFuture.cancel(true));
        pool.awaitTermination(1, TimeUnit.MINUTES);
        assertTrue(pool.isTerminated());
    }

    /**
     * Test cancellation
     *
     * @throws InterruptedException
     * @throws net.redvoiss.sms.smpp.cache.SelfExpiringCacheException
     */
    @Test
    public void testRemovalUsingIntegerAsKey() throws InterruptedException, SelfExpiringCacheException {
        SelfExpiringCache<Integer, String> test = new SelfExpiringCache<>("test-removal");
        Future<?> monitoringTaskFuture = test.activateMonitoringTask(pool);
        assertFalse(test.containsKey(1));
        final String value = "Test";
        test.put(1, value);
        String s = test.remove(1);
        assertEquals(value, s);
        pool.shutdown();
        assertTrue(monitoringTaskFuture.cancel(true));
        pool.awaitTermination(1, TimeUnit.MINUTES);
        assertTrue(pool.isTerminated());
    }

    /**
     * Test cancellation
     *
     * @throws InterruptedException
     * @throws net.redvoiss.sms.smpp.cache.SelfExpiringCacheException
     */
    @Test
    public void testRemovalUsingStringAsKey() throws InterruptedException, SelfExpiringCacheException {
        SelfExpiringCache<String, String> test = new SelfExpiringCache<>("test-removal");
        Future<?> monitoringTaskFuture = test.activateMonitoringTask(pool);
        final String key = "key";
        assertFalse(test.containsKey(key));
        final String value = "Test";
        test.put(key, value);
        String s = test.remove(key);
        assertEquals(value, s);
        pool.shutdown();
        assertTrue(monitoringTaskFuture.cancel(true));
        pool.awaitTermination(1, TimeUnit.MINUTES);
        assertTrue(pool.isTerminated());
    }
}
