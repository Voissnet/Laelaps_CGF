package net.redvoiss.sms.management;

public interface SMSCMBean {

    public String printShortMessageStoreContents();

    public void enablePDUProcessorDisplayInfo(int i);

    public void disablePDUProcessorDisplayInfo(int i);

    public void enableEventTrace();

    public void disableEventTrace();

}
