package net.redvoiss.sms.gateway.gsm.lyric;

import net.redvoiss.sms.SMSException;

public interface Result {
    boolean isSuccess() throws SMSException;
    ErrorCode getErrorCode() throws SMSException;

    enum ErrorCode {
        UnknownErrorCode("Unknown error code"), 
        DatabaseProblemORIdNotFound("Database couldn't be accessed or ticket does not exist"),
        LogError("Internal error while trying to access a queue"),
        AppAnswerProblem("Application does not answer"),
        ContentTooLong("Content was Too Long"),
        APIDisabled("Access API was disabled");
        
        private String m_description;

        ErrorCode( String description ) {
            m_description = description;
        }

        public String getDescription() {
            return m_description;
        }

        public static ErrorCode forError(String s) {
            for (ErrorCode ec :ErrorCode.values()) {
                if (ec.name().equals(s) ) {
                    return ec;
                }
            }
            System.err.println( String.format("Unknown error code was: {%s}", s) ); //TODO get rid of this
            return UnknownErrorCode;
        }
    }
}