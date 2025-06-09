package net.redvoiss.sms.smpp.mbean;

/**
 *
 * @author Jorge Avila
 */
public interface EsmeStatisticMXBean {

    /**
     * Counts number of outgoing submits emitted
     *
     * @return number of submit processed
     */
    public int getSubmitCount();

    /**
     * Counts number of incoming submit responses processed
     *
     * @return number of submit responses processed
     */
    public int getSubmitResponseCount();

    /**
     * Counts number of DLRs received
     *
     * @return number of DLRs processed
     */
    public int getDLRCount();

    /**
     * Counts number of errors
     *
     * @return number of error received
     */
    public int getErrorCount();
}
