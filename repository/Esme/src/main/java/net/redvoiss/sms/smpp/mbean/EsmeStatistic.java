package net.redvoiss.sms.smpp.mbean;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Jorge Avila
 */
public class EsmeStatistic implements EsmeStatisticMXBean {

    private static final AtomicInteger SUBMIT_COUNT = new AtomicInteger(0), SUBMIT_RESPONSE_COUNT = new AtomicInteger(0), DLR_COUNT = new AtomicInteger(0), ERROR_COUNT = new AtomicInteger(0);

    @Override
    public int getSubmitCount() {
        return SUBMIT_COUNT.get();
    }

    /**
     * Increments count
     *
     * @return
     */
    protected int incrementSubmitCount() {
        return SUBMIT_COUNT.incrementAndGet();
    }

    @Override
    public int getSubmitResponseCount() {
        return SUBMIT_RESPONSE_COUNT.get();
    }

    /**
     * Increments count
     *
     * @return
     */
    protected int incrementSubmitResponseCount() {
        return SUBMIT_RESPONSE_COUNT.incrementAndGet();
    }

    @Override
    public int getDLRCount() {
        return DLR_COUNT.get();
    }

    /**
     * Increments count
     *
     * @return
     */
    protected int incrementDLRCount() {
        return DLR_COUNT.incrementAndGet();
    }

    @Override
    public int getErrorCount() {
        return ERROR_COUNT.get();
    }

    /**
     * Increments count
     *
     * @return
     */
    protected int incrementErrorCount() {
        return ERROR_COUNT.incrementAndGet();
    }
}
