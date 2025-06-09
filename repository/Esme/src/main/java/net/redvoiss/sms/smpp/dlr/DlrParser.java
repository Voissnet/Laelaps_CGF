/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.redvoiss.sms.smpp.dlr;

import java.text.ParseException;
import java.util.Date;
import java.util.Optional;

/**
 *
 * @author Jorge Avila
 */
public interface DlrParser {

    /**
     * Parses field
     *
     * @return parsed field
     */
    Optional<String> parseDlvrdField();

    /**
     * Parses field
     *
     * @return parsed field
     * @throws java.text.ParseException
     */
    Optional<Date> parseDoneDateField() throws ParseException;

    /**
     * Parses field
     *
     * @return parsed field
     */
    Optional<String> parseErrField();

    /**
     * Parses field
     *
     * @return parsed field
     */
    Optional<String> parseIdField();

    /**
     * Parses field
     *
     * @return parsed field
     */
    Optional<StatEnum> parseStatField();

    /**
     * Parses field
     *
     * @return parsed field
     */
    Optional<String> parseSubField();

    /**
     * Parses field
     *
     * @return parsed field
     * @throws java.text.ParseException
     */
    Optional<Date> parseSubmitDateField() throws ParseException;

    /**
     * Parses field
     *
     * @return parsed field
     */
    Optional<String> parseTextField();

    /**
     * Lists field options
     */
    public enum StatEnum {

        /**
         * Message is in accepted state
         */
        ACCEPTD,
        /**
         * Message is delivered to destination
         */
        DELIVRD,
        /**
         * Message is in invalid state
         */
        UNKNOWN,
        /**
         * Message validity period has expired
         */
        EXPIRED,
        /**
         * Message has been deleted
         */
        DELETED,
        /**
         * Message is undeliverable
         */
        UNDELIV,
        /**
         * Message is in rejected state
         */
        REJECTD;
    }
}
