package net.redvoiss.sms.smpp.bean;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 *
 * @author Jorge Avila
 */
public interface Message {

    /**
     * Message id
     *
     * @return
     */
    MessageId getId();

    /**
     * Source address used to send message
     *
     * @return
     */
    String getSourceAddress();

    /**
     * Destination address used to send message
     *
     * @return
     */
    String getDestinationAddress();

    /**
     * Message content
     *
     * @return
     */
    String getContent();

    /**
     * Message encoding
     *
     * @return
     */
    String getEncoding();

    /**
     * Message id
     */
    class MessageId {

        private final String primary, secondary;

        /**
         * Constructor
         *
         * @param primaryKey
         * @param secondaryKey
         */
        public MessageId(String primaryKey, String secondaryKey) {
            primary = primaryKey;
            secondary = secondaryKey;
        }

        /**
         * SmsCode
         *
         * @return primary code
         */
        public String getPrimary() {
            return primary;
        }

        /**
         * SmscId
         *
         * @return secondary code
         */
        public String getSecondary() {
            return secondary;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }
}
