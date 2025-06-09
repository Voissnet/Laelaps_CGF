package net.redvoiss.sms.gateway.gsm.teles;

public enum ReceiptTypeEnum {
    SENT("sent (\\d{15})"), FAILED("failed (\\d+) (\\d{15})"), REPLACED("delivery unconfirmed(?:(, status (\\d+),| for \\d+ seconds)) (\\d{15})"), UNCONFIRMED("SMS replaced by SC (\\d{15})"), CONFIRMED("delivery confirmed (\\d{15})");

    String m_regex;

    ReceiptTypeEnum( String regex ) {
        m_regex = regex;
    }

    public String getRegex() {
        return m_regex;
    }
}