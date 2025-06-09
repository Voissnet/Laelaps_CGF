package net.redvoiss.sms.gateway.gsm.lyric;

import net.redvoiss.sms.SMSException;

public interface GAO {
    static final String API_VERSION = "0.08";

    Result deleteMessage(int id) throws SMSException;

    MessageStatusResult getMessageStatus(int id) throws SMSException;

    ChannelStatusResult getChannelStatus() throws SMSException;

    SendMessageResult sendMessage(String destination, String msg) throws SMSException;

    ReceiveMessageResult receiveMessage() throws SMSException;

    Result deleteReadMessages() throws SMSException;

    Result resetQueue() throws SMSException;

    QueueStatusResult getQueueStatus() throws SMSException;

    VersionResult getVersion() throws SMSException;

    static GAO buildNative(String hostname, int port) {
        return new LyricNativeSSLCommunicationImpl(hostname, port);
    }

    static GAO buildPlainNative(String hostname, int port) {
        return new LyricNativePlainCommunicationImpl(hostname, port);
    }

    static GAO buildNative(String username, String password, String hostname, int port) {
        return new LyricNativeSSLCommunicationImpl(username, password, hostname, port);
    }

    static GAO buildNative(String userInfo, String hostname, int port) {
        return new LyricNativePlainCommunicationImpl(userInfo, hostname, port);
    }

    enum DeliveryStatus {
        UNEXPECTED(-1, "Unexpected"), DELIVERED(0, "Message delivered"), FORWARDED(1, "Message forwarded by the SC but unable to confirm delivery "),
        REPLACED(2, "Message replaced by the SC"), CONGESTION(32, "Congestion"), REJECTED(35, "Service rejected"),
        SPECIFIC(48, "Specific to each SC "), INCOMPATIBLE_DESTINATION(65, "Incompatible destination"),
        SM_DOES_NOT_EXIST(73, "Short Message does not exist"), NO_RESPONSE_FROM_SME(98, "No response from SME");

        private int m_status;
        private String m_description;

        DeliveryStatus(int status, String description) {
            m_status = status;
            m_description = description;
        }

        public int getStatus() {
            return m_status;
        }

        public String getDescription() {
            return m_description;
        }

        public static DeliveryStatus forStatus(int status) {
            for (DeliveryStatus deliveryStatus : DeliveryStatus.values()) {
                if (deliveryStatus.getStatus() == status) {
                    return deliveryStatus;
                }
            }
            return UNEXPECTED;
        }
    }

    enum LastError {
        UNEXPECTED(-1), NONE(0), UNKNOWN(1), DESTINATION_NUMBER(2), CONTENT(3), NETWORK(4), SIMCARD(5);

        private int m_error;

        LastError(int error) {
            m_error = error;
        }

        public int getError() {
            return m_error;
        }

        public static LastError forError(int error) {
            for (LastError lastError : LastError.values()) {
                if (lastError.getError() == error) {
                    return lastError;
                }
            }
            return UNEXPECTED;
        }
    }

    enum MessageStatus {
        UNEXPECTED(-1), NEW(0), PROCESSING(1), SENT(2), FAILURE(3);

        private int m_status;

        MessageStatus(int status) {
            m_status = status;
        }

        public int getStatus() {
            return m_status;
        }

        public static MessageStatus forStatus(int status) {
            for (MessageStatus messageStatus : MessageStatus.values()) {
                if (messageStatus.getStatus() == status) {
                    return messageStatus;
                }
            }
            return UNEXPECTED;
        }

        @Override
        public String toString() {
            return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this);
        }
    }

    enum ReportStage {
        UNEXPECTED(-1, "Unexpected"), NONE(0, "No status report has been received yet"), TEMPORARY(1, "Temporary status report"),
        FINAL(2, "Final status report");

        private int m_status;
        private String m_description;

        ReportStage(int status, String description) {
            m_status = status;
            m_description = description;
        }

        public int getStatus() {
            return m_status;
        }
        
        public String getDescription() {
        	return m_description;
        }

        public static ReportStage forStatus(int status) {
            for (ReportStage reportStage : ReportStage.values()) {
                if (reportStage.getStatus() == status) {
                    return reportStage;
                }
            }
            return UNEXPECTED;
        }
    }
}