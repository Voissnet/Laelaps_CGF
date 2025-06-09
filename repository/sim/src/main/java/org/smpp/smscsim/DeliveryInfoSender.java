/*
 * Copyright (c) 1996-2001
 * Logica Mobile Networks Limited
 * All rights reserved.
 *
 * This software is distributed under Logica Open Source License Version 1.0
 * ("Licence Agreement"). You shall use it and distribute only in accordance
 * with the terms of the License Agreement.
 *
 */
package org.smpp.smscsim;

import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;

import org.smpp.Data;
import org.smpp.SmppObject;
import org.smpp.debug.Debug;
import org.smpp.pdu.DeliverSM;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.PDUException;
import org.smpp.pdu.WrongLengthOfStringException;
import org.smpp.util.ProcessingThread;
import org.smpp.util.Queue;

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
//import java.io.ValueNotSetException;
        
/**
 * Class <code>DeliveryInfoSender</code> ...
 *
 * @author Logica Mobile Networks SMPP Open Source Team
 * @version $Revision$
 * @see SimulatorPDUProcessor
 */
public class DeliveryInfoSender extends ProcessingThread {
        protected static final Logger LOGGER = Logger.getLogger(DeliveryInfoSender.class.getName() );
        
	public static final int DELIVERED = 0;
	public static final int EXPIRED = 1;
	public static final int DELETED = 2;
	public static final int UNDELIVERABLE = 3;
	public static final int ACCEPTED = 4;
	public static final int UNKNOWN = 5;
	public static final int REJECTED = 6;

        public static int MAX_DLR_TRANSMITTED = 0;
        private static boolean isBOUND = false;
        private static int MAX_DLR = 50000;
        
	private static final String DLVR_INFO_SENDER_NAME = "DlvrInfoSender";
	private static int dlvrInfoSenderIndex = 0;

	private static final String DELIVERY_RCPT_DATE_FORMAT = "yyMMddHHmm";

	private SimpleDateFormat dateFormatter = new SimpleDateFormat(DELIVERY_RCPT_DATE_FORMAT);

	private long waitForQueueInterval = 5000; // in ms

	private Debug debug = SmppObject.getDebug();

        public int DLR_DELAY = 0;  // Retardo en milisegundos para enviar DLR, especialmente cuando estan acumulados.
        
	private static String[] states;

	static {
		states = new String[7];
		states[DELIVERED] = "DELIVRD";
		states[EXPIRED] = "EXPIRED";
		states[DELETED] = "DELETED";
		states[UNDELIVERABLE] = "UNDELIV";
		states[ACCEPTED] = "ACCEPTD";
		states[UNKNOWN] = "UNKNOWN";
		states[REJECTED] = "REJECTD";
	}

	private Queue submitRequests = new Queue();
	private Queue submittedRequests = new Queue();

	public Queue getQueue() {
		return submitRequests;
	}

	public void submit(PDUProcessor processor, SubmitSM submitRequest, String messageId, int stat, int err) {
		DeliveryInfoEntry entry = new DeliveryInfoEntry(processor, submitRequest, stat, err, messageId);

                /*if (getIsbound()==false) {
                    LOGGER.warning("--> En DeliveryInfoSender.submit(), connection SMPP NOT BOUND");
                    return;
                } *///else 
                //    LOGGER.warning("--> En DeliveryInfoSender.submit(), connection SMPP IS BOUND");
                
		submitRequests.enqueue(entry);
      	}

	public void submit(PDUProcessor processor, SubmitSM submitRequest, String messageId) {
            
                /*if (getIsbound()==false) {
                    LOGGER.warning("--> En DeliveryInfoSender.submit2(), connection SMPP NOT BOUND");
                    return;
                } *///else 
                //    LOGGER.warning("--> En DeliveryInfoSender.submit2(), connection SMPP IS BOUND");

		submit(processor, submitRequest, messageId, DELIVERED, 0);
	}

	protected void deliver(DeliveryInfoEntry entry) throws UnsupportedEncodingException, IOException, Exception, PDUException {
		debug.enter(this, "deliver");
		SubmitSM submit = entry.submit;
		DeliverSM deliver = new DeliverSM();
                
                deliver.setEsmClass((byte)Data.SM_SMSC_DLV_RCPT_TYPE);                
		deliver.setSourceAddr(submit.getSourceAddr());
		deliver.setDestAddr(submit.getDestAddr());

		try { 
			deliver.setReceiptedMessageId(entry.messageId); 
		} catch (org.smpp.pdu.tlv.WrongLengthException e) { 
			e.printStackTrace(); 
		}
		deliver.setMessageState((byte)Data.SM_STATE_ACCEPTED);
                
                //deliver.setDataCoding((byte) 0x03); // ISO-Latin-1
		String msg = "";
		msg += "id:" + entry.messageId + " ";
		msg += "sub:" + entry.sub + " ";
		msg += "dlvrd:" + entry.dlvrd + " ";
		msg += "submit date:" + formatDate(entry.submitted) + " ";
		msg += "done date:" + formatDate(System.currentTimeMillis()) + " ";
		msg += "stat:" + states[entry.stat] + " ";
		msg += "err:" + entry.err + " ";
		String shortMessage = submit.getShortMessage();
		if (shortMessage == null) {
			msg += "text:";
		} else {
			int msgLen = shortMessage.length();
			msg += "text:" + shortMessage.substring(0, (msgLen > 20 ? 20 : msgLen));
		}
		try {
			deliver.setShortMessage(msg);
			deliver.setServiceType(submit.getServiceType());
		} catch (WrongLengthOfStringException e) {
			e.printStackTrace();
		}
		try {
                    DLR_DELAY = Integer.parseInt(System.getProperty("DLR_DELAY"));                   
                    //CGF 20230414. To delay sending of DLR for avoiding stuck of server and problem with BINDRESP after a conecction lost.
                    if (DLR_DELAY>0)
                    {
                        try {
                                Thread.sleep(DLR_DELAY);
                        } catch (InterruptedException e) {
                                //e.printStackTrace();
                        }
                    }                            

		    try {
			entry.processor.serverRequest(deliver);
                        LOGGER.warning( "DeliveryinfoSender.deliver()  sent serverRequest DELIVER ok ");
                        
		    } catch (IOException e) {
                        setIsbound(false);
                        LOGGER.warning( "DeliveryinfoSender.deliver() caught IOException from entry.processor.serverRequest(deliver) " );   
                        throw new IOException();
                        
		    } catch (PDUException e) {
                        setIsbound(false);
                        LOGGER.warning( "DeliveryinfoSender.deliver() caught PDUException from entry.processor.serverRequest(deliver) " );   
                        throw new PDUException();

		    } catch (Exception e) {
                        setIsbound(false);
                        LOGGER.warning( "DeliveryinfoSender.deliver() caught Exception from entry.processor.serverRequest(deliver) " );   
                        throw new Exception();
                        
		    }                           
                        
		} catch (PDUException pdue) {
			pdue.printStackTrace();
		//} catch (IOException ioe) {
		//	entry.processor.stop();
		}
		debug.exit(this);
	}

        public static void setIsbound(boolean isbound)
        {
            isBOUND = isbound;
        }

        public static boolean getIsbound()
        {
            return isBOUND ;
        }
        
        public void process()   {
            
                DLR_DELAY = Integer.parseInt(System.getProperty("DLR_DELAY"));
            
		if (submitRequests.isEmpty()) {
			try {
				synchronized (submitRequests) {
					submitRequests.wait(waitForQueueInterval);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
                    //int nDLRs = 0;
			//while (nDLRs<MAX_DLR && !submitRequests.isEmpty()) {
			while (!submitRequests.isEmpty()) {
                            try {
                                //LOGGER.warning("Processing a DELIVER from submitRequests Queue");
                                //if (getIsbound()==true)
      				    deliver((DeliveryInfoEntry) submitRequests.dequeue());
                                //else
                                //    LOGGER.warning("Connection SMPP not bound in process()");
                                
                            } catch ( PDUException e ) {
                                LOGGER.warning("PDUException in DeliveryinfoSender.process()");
                                
                                //throw new PDUException();
                            } catch ( IOException e ) {
                                LOGGER.warning("IOException in DeliveryinfoSender.process()");
                                //throw new IOException();
                                
                            } catch ( Exception e ) {
                                LOGGER.warning("Exception in DeliveryinfoSender.process()");
                                //throw new Exception();
                            }
                            //nDLRs++;
			}
		}
	}

	public String getThreadName() {
		return DLVR_INFO_SENDER_NAME;
	}

	public int getThreadIndex() {
		return ++dlvrInfoSenderIndex;
	}

	private String formatDate(long ms) {
		synchronized (dateFormatter) {
			return dateFormatter.format(new Date(ms));
		}
	}

	protected class DeliveryInfoEntry {
		public PDUProcessor processor;
		public SubmitSM submit;
		public int sub = 1;
		public int dlvrd = 1;
		public int stat;
		public int err;
		public String messageId;
		public long submitted = System.currentTimeMillis();  //CGF , esto debo cambiar para DLR se vaya con fechahora del operador

		public DeliveryInfoEntry(PDUProcessor processor, SubmitSM submit, int stat, int err, String messageId) {
			this.processor = processor;
			this.submit = submit;
			this.stat = stat;
			this.err = err;
			this.messageId = messageId;
                        //try {
                        //    this.submitted = submit.getDisplayTime();
                        //} catch(Exception e) {
                        //    this.submitted = System.currentTimeMillis();
                        //}
		}
	}
}
/*
 * $Log$
 */
