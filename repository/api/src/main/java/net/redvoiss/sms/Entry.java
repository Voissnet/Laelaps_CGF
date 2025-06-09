package net.redvoiss.sms;

public interface Entry {  
    String getUsername();
    String getDestination();
    String getMessage();
    String getDescription();
    String getMessageParameter();
    boolean isBussinesType();
}