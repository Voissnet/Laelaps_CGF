/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.redvoiss.sms.smpp.dlr;

/**
 *
 * @author Jorge Avila
 */
public interface AbstractDlrParserFactory {

    /**
     * Creates parser
     * @param dlr
     * @return parsed object
     */
    public DlrParser createDlrParser(String dlr);

}
