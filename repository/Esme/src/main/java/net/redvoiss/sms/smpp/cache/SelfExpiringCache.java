package net.redvoiss.sms.smpp.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.redvoiss.sms.smpp.SubmitContext;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.DAYS;

/**
 * Alerts when entry is removed
 *
 * @author Jorge Avila
 * @param <K> key type
 * @param <V> value type
 */
public class SelfExpiringCache<K, V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SelfExpiringCache.class);
    private final DelayQueue<DelayedKey> delayQueue = new DelayQueue<>();
    private final Runnable monitoringTask;

    /**
     * Map
     */
    protected final Map<K, V> map = new ConcurrentHashMap<>();

    /**
     * Obtains future in case cancellation is necessary
     *
     * @param pool
     * @return
     */
    public Future<?> activateMonitoringTask(ExecutorService pool) {
        return pool.submit(monitoringTask);
    }

    /**
     * Creates self expiring cache object
     *
     * @param name
     */
    public SelfExpiringCache(String name) {
        monitoringTask = () -> {
            Thread.currentThread().setName(name + "-self-expiring-cache");
            try {
                while (true) {
                    DelayedKey dk = delayQueue.take();
                    final K k = dk.getId();
                    if (k == null) {
                        LOGGER.error("Unexpected empty key");
                    } else {
                        final V v = map.remove(k);
                        if (v == null) {
                            LOGGER.error("Unexpected empty value for key {}", k);
                        } else {
                            LOGGER.warn("Due to expiration removing {} -> {}", k, v);
                        }
                    }
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Unexpected interruption", ie);
            }
        };
    }

    /**
     * Verifies key is present
     *
     * @param key
     * @return
     */
    public boolean containsKey(K key) {
        synchronized (this) {
            return map.containsKey(key);
        }
    }

    /**
     * Adds key/pair value
     *
     * @param key
     * @param value
     * @return
     * @throws SelfExpiringCacheException
     */
    public V put(K key, V value) throws SelfExpiringCacheException {
        synchronized (this) {
            boolean addOk = delayQueue.add(new DelayedKey(key));
            if (addOk) {
                return map.put(key, value);
            }
            throw new SelfExpiringCacheException();
        }
    }

    /**
     * Adds key/pair value
     *
     * @param submitContext
     * @param key
     * @param value
     * @return
     * @throws SelfExpiringCacheException
     */
    public V put(SubmitContext submitContext, K key, V value) throws SelfExpiringCacheException {
        return put(key, value);
    }

    /**
     * Removes key/pair value
     *
     * @param key
     * @return
     * @throws SelfExpiringCacheException
     */
    public V remove(K key) throws SelfExpiringCacheException {
        synchronized (this) {
            boolean removeOk = delayQueue.remove(new DelayedKey(key));
            if (removeOk) {
                return map.remove(key);
            }
            throw new SelfExpiringCacheException();
        }
    }

    /**
     * Obtains pair value from key
     *
     * @param key
     * @return
     */
    public V get(K key) {
        return map.get(key);
    }

    private class DelayedKey implements Delayed {

        K id;
        private final long DELAY = System.nanoTime() + NANOSECONDS.convert(1, DAYS);

        DelayedKey(K key) {
            id = key;
        }

        K getId() {
            return id;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(DELAY - System.nanoTime(), NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return o == this ? 0
                    : Long.compare(this.getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));

        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(id).toHashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            if (obj instanceof SelfExpiringCache.DelayedKey) {
                final SelfExpiringCache.DelayedKey other = (SelfExpiringCache.DelayedKey) obj;
                return new EqualsBuilder()
                        .append(id, other.id)
                        .isEquals();
            }
            return false;
        }

    }
}
