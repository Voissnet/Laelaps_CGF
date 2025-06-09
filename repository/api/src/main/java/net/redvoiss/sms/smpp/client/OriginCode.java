package net.redvoiss.sms.smpp.client;

public class OriginCode {
    private String m_smsCode, m_msgIdGW;
    private int m_route;
    private long m_creationTime;

    protected OriginCode(String smsCode, String msgIdGW, int route) {
        m_smsCode = smsCode;
        m_msgIdGW = msgIdGW;
        m_route = route;
        m_creationTime = System.nanoTime();
    }

    OriginCode(String smsCode, String msgIdGW) {
        m_smsCode = smsCode;
        m_msgIdGW = msgIdGW;
        m_creationTime = System.nanoTime();
    }

    String getSmsCode() {
        return m_smsCode;
    }

    String getMsgIdGW() {
        return m_msgIdGW;
    }

    int getRoute() {
        return m_route;
    }

    long getElapsedTime() {
        return System.nanoTime() - m_creationTime;
    }
}