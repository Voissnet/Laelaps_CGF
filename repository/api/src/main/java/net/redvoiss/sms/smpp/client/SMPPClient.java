package net.redvoiss.sms.smpp.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.smpp.Data;
import org.smpp.debug.Debug;
import org.smpp.debug.Event;
import org.smpp.debug.FileDebug;
import org.smpp.debug.FileEvent;
import org.smpp.pdu.Address;
import org.smpp.pdu.AddressRange;
import org.smpp.pdu.BindReceiver;
import org.smpp.pdu.BindRequest;
import org.smpp.pdu.BindResponse;
import org.smpp.pdu.BindTransciever;
import org.smpp.pdu.BindTransmitter;
import org.smpp.pdu.CancelSM;
import org.smpp.pdu.CancelSMResp;
import org.smpp.pdu.DataSM;
import org.smpp.pdu.DataSMResp;
import org.smpp.pdu.DeliverSM;
import org.smpp.pdu.DestinationAddress;
import org.smpp.pdu.EnquireLink;
import org.smpp.pdu.EnquireLinkResp;
import org.smpp.pdu.PDU;
import org.smpp.pdu.QuerySM;
import org.smpp.pdu.QuerySMResp;
import org.smpp.pdu.ReplaceSM;
import org.smpp.pdu.ReplaceSMResp;
import org.smpp.pdu.Request;
import org.smpp.pdu.Response;
import org.smpp.ServerPDUEvent;
import org.smpp.ServerPDUEventListener;
import org.smpp.Session;
import org.smpp.SmppObject;
import org.smpp.pdu.SubmitMultiSM;
import org.smpp.pdu.SubmitMultiSMResp;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.SubmitSMResp;
import org.smpp.TCPIPConnection;
import org.smpp.pdu.UnbindResp;
import org.smpp.pdu.WrongLengthOfStringException;
import org.smpp.util.Queue;

import net.redvoiss.sms.dao.DAO;

import javax.sql.DataSource;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Level.SEVERE;
import org.smpp.util.ByteBuffer;

public abstract class SMPPClient implements Callable {

    /**
     * Revoiss instance variables
     */
    private DataSource m_ds;
    private static final Logger LOGGER = java.util.logging.Logger.getLogger(SMPPClient.class.getName());
    private Map<Integer, OriginCode> m_smsCodeFromSequenceMap = new LinkedHashMap<Integer, OriginCode>();
    private Map<String, OriginCode> m_smsCodeFromSMSCID = new LinkedHashMap<String, OriginCode>();
    private static final String PREFIX_REGEX = "^56";
    private static final String MANIFEST_FILE_NAME = "META-INF/MANIFEST.MF";
    private static final int SLEEP_SECS = 1;
    private static final String QUERY = DAO.loadProperties().getProperty("net.redvoiss.sms.dao.check.sms");
    //private static final String QUERY = DAO.loadProperties().getProperty("net.redvoiss.sms.dao.check.sms_udh"); // leo ta,bien esm_class y udh_header    
    private int m_codeRuta;
    private String m_idoidd;

    // END Redvoiss settings
    static final String copyright = "Copyright (c) 1996-2001 Logica Mobile Networks Limited\n"
            + "This product includes software developed by Logica by whom copyright\n"
            + "and know-how are retained, all rights reserved.\n";
    static final String version = "SMPP Open Source test & demonstration application, version 1.1\n";

    static {
        LOGGER.config(copyright);
        LOGGER.config(version);
    }

    /**
     * Directory for creating of debug and event files.
     */
    static final String dbgDir = System.getProperty("smpp.dbg.dir", "./");

    /**
     * The debug object.
     *
     * @see FileDebug
     */
    static Debug debug = new FileDebug(dbgDir, "client.dbg");

    /**
     * The event object.
     *
     * @see FileEvent
     */
    static Event event = new FileEvent(dbgDir, "client.evt");

    /**
     * File with default settings for the application.
     */
    static String propsFilePath = System.getProperty("smpp.default.cfg", "./smpptest.cfg");

    /**
     * This is the SMPP session used for communication with SMSC.
     */
    static Session session = null;

    /**
     * Contains the parameters and default values for this test application such
     * as system id, password, default npi and ton of sender etc.
     */
    Properties properties = new Properties();

    /**
     * If the application is bound to the SMSC.
     */
    boolean bound = false;

    /**
     * If the application has to keep reading commands from the keyboard and to
     * do what's requested.
     */
    private boolean keepRunning = true;

    /**
     * Address of the SMSC.
     */
    String ipAddress = null;

    /**
     * The port number to bind to on the SMSC server.
     */
    int port = 0;

    /**
     * The name which identifies you to SMSC.
     */
    String systemId = null;

    /**
     * The password for authentication to SMSC.
     */
    String password = null;

    /**
     * How you want to bind to the SMSC: transmitter (t), receiver (r) or
     * transciever (tr). Transciever can both send messages and receive
     * messages. Note, that if you bind as receiver you can still receive
     * responses to you requests (submissions).
     */
    String bindOption = "t";

    /**
     * Indicates that the Session has to be asynchronous. Asynchronous Session
     * means that when submitting a Request to the SMSC the Session does not
     * wait for a response. Instead the Session is provided with an instance of
     * implementation of ServerPDUListener from the smpp library which receives
     * all PDUs received from the SMSC. It's application responsibility to match
     * the received Response with sended Requests.
     */
    boolean asynchronous = false;

    /**
     * This is an instance of listener which obtains all PDUs received from the
     * SMSC. Application doesn't have explicitly call Session's receive()
     * function, all PDUs are passed to this application callback object. See
     * documentation in Session, Receiver and ServerPDUEventListener classes
     * form the SMPP library.
     */
    SMPPTestPDUEventListener pduListener = null;

    /**
     * The range of addresses the smpp session will serve.
     */
    AddressRange addressRange = new AddressRange();

    /*
	 * for information about these variables have a look in SMPP 3.4 specification
     */
    String systemType = "";
    String serviceType = "";
    Address sourceAddress = new Address();
    Address destAddress = new Address();
    String scheduleDeliveryTime = "";
    String validityPeriod = "";
    String shortMessage = "";
    int numberOfDestination = 1;
    String messageId = "";
    byte esmClass = 0;
    byte protocolId = 0;
    byte priorityFlag = 0;
    byte registeredDelivery = Data.SM_SMSC_RECEIPT_REQUESTED;
    byte replaceIfPresentFlag = 0;
    byte dataCoding = Data.DFLT_DATA_CODING;
    byte smDefaultMsgId = 0;

    /**
     * If you attemt to receive message, how long will the application wait for
     * data.
     */
    long receiveTimeout = Data.RECEIVE_BLOCKING;

    /**
     * Initialises the application, lods default values for connection to SMSC
     * and for various PDU fields.
     */
    public SMPPClient(int codeRuta, String idoidd) throws IOException, SQLException {
        LOGGER.config(SMPPClient.getRelease());
        loadProperties(propsFilePath);
        m_ds = DAO.buildOracleDataSource();
        m_codeRuta = codeRuta;
        m_idoidd = idoidd;
    }

    protected static String getRelease() {
        String ret = "Unable to read release details";
        try {
            java.util.Enumeration<java.net.URL> urls = SMPPClient.class.getClassLoader()
                    .getResources(MANIFEST_FILE_NAME);
            while (urls.hasMoreElements()) {
                java.net.URL url = urls.nextElement();
                try (java.io.InputStream manifestStream = url.openStream()) {
                    java.util.jar.Manifest manifest = new java.util.jar.Manifest(manifestStream);
                    java.util.jar.Attributes attrs = manifest.getMainAttributes();
                    String ib = attrs.getValue("Implementation-Build");
                    String ibb = attrs.getValue("Implementation-Build-Branch");
                    if (ib == null && ibb == null) {
                        LOGGER.finest(
                                () -> String.format("Unable to read release details from {%s}", url.toExternalForm()));
                        /*
						 * if (false) { for (java.util.Map.Entry<Object, Object> m : attrs.entrySet()) {
						 * LOGGER.info(String.format("{%s}: {%s} -> {%s}", url.toExternalForm(),
						 * String.valueOf(m.getKey()), String.valueOf(m.getValue()))); } }//
                         */
                    } else {
                        ret = String.format("Running using commit {%s} on branch {%s}", ib, ibb);
                        break;
                    }
                }
            }
        } catch (java.io.IOException ioe) {
            LOGGER.log(WARNING, String.format("Unable to find {%s}", MANIFEST_FILE_NAME), ioe);
        }
        return ret;
    }

    /**
     * Sets global SMPP library debug and event objects. Runs the application.
     *
     * @see SmppObject#setDebug(Debug)
     * @see SmppObject#setEvent(Event)
     */
    public Object call() throws Exception {
        LOGGER.info("Initialising...");
        java.util.concurrent.ScheduledExecutorService executorService = java.util.concurrent.Executors
                .newScheduledThreadPool(2);
        try {
            bind();
            if (bound) {
                java.util.concurrent.Future<?> receiveFuture = executorService.submit(new Thread() {
                    public void run() {
                        while (bound && keepRunning) {
                            receive();
                        }
                        LOGGER.info("No longer receiving PDU's");
                    }
                });
                java.util.concurrent.Future<?> enquireLinkFuture = executorService.scheduleAtFixedRate(() -> {
                    if (bound) {
                        enquireLink();
                    }
                }, 0, getIntProperty("enquire-link-recurrence-period", 5), TimeUnit.SECONDS);
                submit();
                enquireLinkFuture.cancel(true);
                receiveFuture.cancel(true);
                executorService.shutdown();
            }
        } finally {
            exit();
            if (executorService.awaitTermination(2, java.util.concurrent.TimeUnit.MINUTES)) {
                LOGGER.info("Shutting down thread pool without issues.");
            } else {
                java.util.List<Runnable> receiverList = executorService.shutdownNow();
                LOGGER.warning("Lingering threads are: " + receiverList);
            }
        }
        return null;
    }

    /**
     * The first method called to start communication betwen an ESME and a SMSC.
     * A new instance of <code>TCPIPConnection</code> is created and the IP
     * address and port obtained from user are passed to this instance. New
     * <code>Session</code> is created which uses the created
     * <code>TCPIPConnection</code>. All the parameters required for a bind are
     * set to the <code>BindRequest</code> and this request is passed to the
     * <code>Session</code>'s <code>bind</code> method. If the call is
     * successful, the application should be bound to the SMSC.
     *
     * See "SMPP Protocol Specification 3.4, 4.1 BIND Operation."
     *
     * @see BindRequest
     * @see BindResponse
     * @see TCPIPConnection
     * @see Session#bind(BindRequest)
     * @see Session#bind(BindRequest,ServerPDUEventListener)
     */
    private void bind() {
        debug.enter(this, "SMPPTest.bind()");
        try {

            if (bound) {
                LOGGER.warning("Already bound, unbind first.");
                return;
            }

            BindRequest request = null;
            BindResponse response = null;
            String syncMode = (asynchronous ? "a" : "s");

            // type of the session
            syncMode = getParam("Asynchronous/Synchronnous Session? (a/s)", syncMode);
            if (syncMode.compareToIgnoreCase("a") == 0) {
                asynchronous = true;
            } else if (syncMode.compareToIgnoreCase("s") == 0) {
                asynchronous = false;
            } else {
                LOGGER.warning("Invalid mode async/sync, expected a or s, got " + syncMode + ". Operation canceled.");
                return;
            }

            // input values
            bindOption = getParam("Transmitter/Receiver/Transciever (t/r/tr)", bindOption);

            if (bindOption.compareToIgnoreCase("t") == 0) {
                request = new BindTransmitter();
            } else if (bindOption.compareToIgnoreCase("r") == 0) {
                request = new BindReceiver();
            } else if (bindOption.compareToIgnoreCase("tr") == 0) {
                request = new BindTransciever();
            } else {
                LOGGER.warning("Invalid bind mode, expected t, r or tr, got " + bindOption + ". Operation canceled.");
                return;
            }

            ipAddress = getParam("IP address of SMSC", ipAddress);
            port = getParam("Port number", port);

            TCPIPConnection connection = new TCPIPConnection(ipAddress, port);
            connection.setReceiveTimeout(20 * 1000);
            session = new Session(connection);

            systemId = getParam("Your system ID", systemId);
            password = getParam("Your password", password);

            // set values
            request.setSystemId(systemId);
            request.setPassword(password);
            request.setSystemType(systemType);
            request.setInterfaceVersion((byte) 0x34);
            request.setAddressRange(addressRange);

            // send the request
            LOGGER.fine("Bind request " + request.debugString());
            if (asynchronous) {
                pduListener = new SMPPTestPDUEventListener(session);
                response = session.bind(request, pduListener);
            } else {
                response = session.bind(request);
            }
            LOGGER.fine("Bind response " + response.debugString());
            if (response.getCommandStatus() == Data.ESME_ROK) {
                bound = true;
            }

        } catch (Exception e) {
            event.write(e, "");
            debug.write("Bind operation failed. " + e);
            LOGGER.log(SEVERE, "Bind operation failed. ", e);
        } finally {
            debug.exit(this);
        }
    }

    /**
     * Ubinds (logs out) from the SMSC and closes the connection.
     *
     * See "SMPP Protocol Specification 3.4, 4.2 UNBIND Operation."
     *
     * @see Session#unbind()
     * @see Unbind
     * @see UnbindResp
     */
    private void unbind() {
        debug.enter(this, "SMPPTest.unbind()");
        try {

            if (!bound) {
                LOGGER.warning("Not bound, cannot unbind.");
                return;
            }

            // send the request
            LOGGER.fine("Going to unbind.");
            if (session.getReceiver().isReceiver()) {
                LOGGER.fine("It can take a while to stop the receiver.");
            }
            UnbindResp response = session.unbind();
            bound = false;
            if (response == null) {
                LOGGER.warning("Empty response during unbind");
            } else {
                LOGGER.fine(() -> "Unbind response " + response.debugString());
            }
        } catch (Exception e) {
            event.write(e, "");
            debug.write("Unbind operation failed. " + e);
            LOGGER.log(SEVERE, "Unbind operation failed. ", e);
        } finally {
            debug.exit(this);
        }
    }

    abstract void set(SubmitSM request, String shortMessage)
            throws WrongLengthOfStringException, UnsupportedEncodingException;

    protected Pattern getDestinationPattern() {
        return Pattern.compile(PREFIX_REGEX + "\\d{9}$");
    }

    /**
     * Creates a new instance of <code>SubmitSM</code> class, lets you set
     * subset of fields of it. This PDU is used to send SMS message to a device.
     *
     * See "SMPP Protocol Specification 3.4, 4.4 SUBMIT_SM Operation."
     *
     * @see Session#submit(SubmitSM)
     * @see SubmitSM
     * @see SubmitSMResp
     */
    private void submit() {
        cycle:
        while (bound) {
            debug.enter(this, "SMPPTest.submit()");
            try (java.sql.Connection conn = m_ds.getConnection()) {
                try {
                    conn.setAutoCommit(false);
                    final long startTime = System.nanoTime();
                    try (java.sql.PreparedStatement ps = conn.prepareStatement(QUERY)) {
                        ps.setString(1, DAO.MessageState.NEW.getState());
                        ps.setInt(2, m_codeRuta);
                        ps.setInt(3, m_codeRuta);
                        ps.setInt(4, 11);
                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                            boolean noEntries = true;
                            while (rs.next()) {
                                noEntries = false;
                                final String smsCode = rs.getString("SMS_COD");
                                final String msgIdGW = rs.getString("MSGID_GW");
                                //final byte esm_class = ((int)rs.getByte("ESM_CLASS")==1 ? (byte)0x40 : (byte)0x00 );
				//final byte[] udh_header = rs.getBytes("UDH_HEADER");
                                                                            
                                //LOGGER.warning("----> SMS smsCode="+smsCode+", destino= "+rs.getString("DST_ADDRS")+", esm_class="+esm_class+", texto='"+rs.getString("SMS_TXT")+"' ");
                                try {
                                    sourceAddress = new Address((byte) 1, (byte) 1, rs.getString("SOURC_ADDRS"));
                                    final String destination = rs.getString("DST_ADDRS");
                                    Matcher contentMatcher = getDestinationPattern().matcher(destination);
                                    if (contentMatcher.matches()) {
                                        if (m_idoidd == null || m_idoidd.length() == 0) {// Classic mode enabled
                                            destAddress = new Address((byte) 1, (byte) 1, destination);
                                        } else {
                                            destAddress = new Address((byte) 0, (byte) 13,
                                                    destination.replaceFirst(PREFIX_REGEX, m_idoidd));
                                        }
                                        shortMessage = rs.getString("SMS_TXT");
                                        // input values
                                        serviceType = getParam("Service type", serviceType);
                                        sourceAddress = getAddress("Source", sourceAddress);
                                        destAddress = getAddress("Destination", destAddress);
                                        replaceIfPresentFlag = getParam("Replace if present flag",
                                                replaceIfPresentFlag);
                                        shortMessage = getParam("The short message", shortMessage);
                                        scheduleDeliveryTime = getParam("Schedule delivery time", scheduleDeliveryTime);
                                        validityPeriod = getParam("Validity period", validityPeriod);
                                        esmClass = getParam("Esm class", esmClass);
                                        protocolId = getParam("Protocol id", protocolId);
                                        priorityFlag = getParam("Priority flag", priorityFlag);
                                        registeredDelivery = getParam("Registered delivery", registeredDelivery);
                                        dataCoding = getParam("Data encoding", dataCoding);
                                        smDefaultMsgId = getParam("Sm default msg id", smDefaultMsgId);

                                        SubmitSM request = new SubmitSM();
                                        // set values
                                        request.setServiceType(serviceType);
                                        request.setSourceAddr(sourceAddress);
                                        request.setDestAddr(destAddress);
                                        request.setReplaceIfPresentFlag(replaceIfPresentFlag);
                                        set(request, shortMessage);
                                        request.setScheduleDeliveryTime(scheduleDeliveryTime);
                                        request.setValidityPeriod(validityPeriod);
					request.setEsmClass(esmClass);
                                        request.setProtocolId(protocolId);
                                        request.setPriorityFlag(priorityFlag);
                                        request.setRegisteredDelivery(registeredDelivery);
                                        request.setSmDefaultMsgId(smDefaultMsgId);
/*
                                        if ( esm_class==0x40 )
                                        {
						ByteBuffer ed = new ByteBuffer();
						ed.appendByte((byte) udh_header[0]);  
						ed.appendByte((byte) udh_header[1]);  
						ed.appendByte((byte) udh_header[2]);  
						ed.appendByte((byte) udh_header[3]);  
						ed.appendByte((byte) udh_header[4]);    
						ed.appendByte((byte) udh_header[5]);  
						ed.appendString(shortMessage, Data.ENC_CP1252);                                                                                        
                                        	request.setShortMessageData(ed); //, Data.ENC_CP1252);                                                                                        
                                        }
*/                                        
                                        // send the request
                                        int count = 1;
                                        count = getParam("How many times to submit this message (load test)", count);
                                        for (int i = 0; i < count; i++) {
                                            request.assignSequenceNumber(true);
                                            LOGGER.fine(
                                                    String.format("#%d  Submit request %s", i, request.debugString()));
                                            try {
                                                if (asynchronous) {
                                                    session.submit(request);
                                                    final int seq = request.getSequenceNumber();
                                                    m_smsCodeFromSequenceMap.put(seq, new OriginCode(smsCode, msgIdGW));
                                                    int rowCount = DAO.storeSequenceNumber(conn, smsCode, seq);
                                                    assert rowCount > 0;
                                                } else {
                                                    SubmitSMResp response = session.submit(request);
                                                    LOGGER.log(FINE, "Submit response {}", response.debugString());
                                                    messageId = response.getMessageId();
                                                    int rowCount = DAO.sent(conn, messageId, smsCode);
                                                    assert rowCount > 0;
                                                }
                                            } catch (IOException ioe) {
                                                LOGGER.log(SEVERE, "Unable to send message, quitting...", ioe);
                                                try {
                                                    conn.rollback();
                                                } catch (java.sql.SQLException re) {
                                                    LOGGER.log(SEVERE, "Rollback", re);
                                                }
                                                break cycle;
                                            }
                                        }
                                    } else {
                                        LOGGER.warning(String.format("Wrong destination {%s} for sms {%s}", destination,
                                                smsCode));
                                        markError(conn, smsCode, msgIdGW);
                                    }
                                } catch (java.sql.SQLException e) {
                                    throw e;
                                } catch (Exception e) {
                                    event.write(e, "");
                                    debug.write("Submit operation failed. " + e);
                                    LOGGER.log(SEVERE, "Submit operation failed. ", e);
                                }
                            }
                            if (noEntries) {
                                LOGGER.finest(() -> String.format("Main thread will sleep for %d seconds", SLEEP_SECS));
                                Thread.sleep(SLEEP_SECS * 1000);
                            }
                        }
                    }
                    conn.commit();
                    final long sleepTimeInMilliseconds = (startTime + 1000000000 - System.nanoTime()) / 1000000; // Quicker
                    // than
                    // one
                    // second
                    if (sleepTimeInMilliseconds > 0) {
                        LOGGER.finest(() -> String.format("Main thread will sleep for %s milliseconds",
                                String.valueOf(sleepTimeInMilliseconds)));
                        Thread.sleep(sleepTimeInMilliseconds);
                    }
                } catch (java.sql.SQLException e) {
                    try {
                        conn.rollback();
                    } catch (java.sql.SQLException re) {
                        LOGGER.log(SEVERE, "Rollback", re);
                    }
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (InterruptedException e) {
                LOGGER.log(WARNING, "Interrupted", e);
            } catch (java.sql.SQLException e) {
                LOGGER.log(WARNING, "DB", e);
            } finally {
                debug.exit(this);
            }
        }
    }

    private void markError(java.sql.Connection conn, String smsCode, String msgIdGW) throws SQLException {

                    try (java.sql.PreparedStatement ps = conn.prepareStatement(
				"insert into FS_ESME_DATA(SMS_COD,DST_ADDRS,SMS_TXT,FECHA_HORA_PROGRAMADA,CODE_RUTA,CODE_SMSC,ERROR,FECHA_HORA_ENVIADO,SOURC_ADDRS)  " +
                                "select e.SMS_COD,e.DST_ADDRS,e.SMS_TXT,e.FECHA_HORA,e.CODE_RUTA,e.CODE_SMSC,1,sysdate,e.SOURC_ADDRS from fs_esme e where e.SMS_COD = ?  ")) {
                      
        //try (java.sql.PreparedStatement ps = conn.prepareStatement(
        //        "insert into FS_ESME_DATA (select e.sms_cod, e.dst_addrs, e.sms_txt, e.fecha_hora, e.code_ruta, e.code_smsc, 1 /*ERROR (legacy)*/, sysdate, e.sourc_addrs from fs_esme e where e.SMS_COD = ? )")) {
            ps.setString(1, smsCode);
            int rowCount = ps.executeUpdate();
            assert rowCount > 0;
        }
        try (java.sql.PreparedStatement ps = conn.prepareStatement("DELETE FROM FS_ESME WHERE SMS_COD=?")) {
            ps.setString(1, smsCode);
            int rowCount = ps.executeUpdate();
            assert rowCount > 0;
        }
        if (msgIdGW == null) {
            LOGGER.fine(String.format("Message {%s} will not generate a DLR", smsCode));
        } else {
            try (java.sql.PreparedStatement ps = conn
                    .prepareStatement("UPDATE FS_SMSCGW SET ID_REPORT=? WHERE MSGID=?")) {
                ps.setString(1, DAO.ReportState.ERROR.getState());
                ps.setString(2, msgIdGW);
                int rowCount = ps.executeUpdate();
                assert rowCount > 0;
            }
        }
    }

    /**
     * Creates a new instance of <code>SubmitMultiSM</code> class, lets you set
     * subset of fields of it. This PDU is used to send SMS message to multiple
     * devices.
     *
     * See "SMPP Protocol Specification 3.4, 4.5 SUBMIT_MULTI Operation."
     *
     * @see Session#submitMulti(SubmitMultiSM)
     * @see SubmitMultiSM
     * @see SubmitMultiSMResp
     */
    private void submitMulti() {
        debug.enter(this, "SMPPTest.submitMulti()");

        try {
            SubmitMultiSM request = new SubmitMultiSM();
            SubmitMultiSMResp response;

            // input values and set some :-)
            serviceType = getParam("Service type", serviceType);
            sourceAddress = getAddress("Source", sourceAddress);
            numberOfDestination = getParam("Number of destinations", numberOfDestination);
            for (int i = 0; i < numberOfDestination; i++) {
                request.addDestAddress(new DestinationAddress(getAddress("Destination", destAddress)));
            }
            replaceIfPresentFlag = getParam("Replace if present flag", replaceIfPresentFlag);
            shortMessage = getParam("The short message", shortMessage);
            scheduleDeliveryTime = getParam("Schdule delivery time", scheduleDeliveryTime);
            validityPeriod = getParam("Validity period", validityPeriod);
            esmClass = getParam("Esm class", esmClass);
            protocolId = getParam("Protocol id", protocolId);
            priorityFlag = getParam("Priority flag", priorityFlag);
            registeredDelivery = getParam("Registered delivery", registeredDelivery);
            dataCoding = getParam("Data encoding", dataCoding);
            smDefaultMsgId = getParam("Sm default msg id", smDefaultMsgId);

            // set other values
            request.setServiceType(serviceType);
            request.setSourceAddr(sourceAddress);
            request.setReplaceIfPresentFlag(replaceIfPresentFlag);
            request.setShortMessage(shortMessage);
            request.setScheduleDeliveryTime(scheduleDeliveryTime);
            request.setValidityPeriod(validityPeriod);
            request.setEsmClass(esmClass);
            request.setProtocolId(protocolId);
            request.setPriorityFlag(priorityFlag);
            request.setRegisteredDelivery(registeredDelivery);
            request.setDataCoding(dataCoding);
            request.setSmDefaultMsgId(smDefaultMsgId);

            // send the request
            LOGGER.fine("Submit Multi request " + request.debugString());
            if (asynchronous) {
                session.submitMulti(request);
            } else {
                response = session.submitMulti(request);
                LOGGER.fine("Submit Multi response " + response.debugString());
                messageId = response.getMessageId();
            }

        } catch (Exception e) {
            event.write(e, "");
            debug.write("Submit Multi operation failed. " + e);
            LOGGER.fine("Submit Multi operation failed. " + e);
        } finally {
            debug.exit(this);
        }
    }

    /**
     * Creates a new instance of <code>ReplaceSM</code> class, lets you set
     * subset of fields of it. This PDU is used to replace certain attributes of
     * already submitted message providing that you 'remember' message id of the
     * submitted message. The message id is assigned by SMSC and is returned to
     * you with the response to the submision PDU (SubmitSM, DataSM etc.).
     *
     * See "SMPP Protocol Specification 3.4, 4.10 REPLACE_SM Operation."
     *
     * @see Session#replace(ReplaceSM)
     * @see ReplaceSM
     * @see ReplaceSMResp
     */
    private void replace() {
        debug.enter(this, "SMPPTest.replace()");
        try {
            ReplaceSM request = new ReplaceSM();
            ReplaceSMResp response;

            // input values
            messageId = getParam("Message id", messageId);
            sourceAddress = getAddress("Source", sourceAddress);
            shortMessage = getParam("The short message", shortMessage);
            scheduleDeliveryTime = getParam("Schedule delivery time", scheduleDeliveryTime);
            validityPeriod = getParam("Validity period", validityPeriod);
            registeredDelivery = getParam("Registered delivery", registeredDelivery);
            smDefaultMsgId = getParam("Sm default msg id", smDefaultMsgId);

            // set values
            request.setMessageId(messageId);
            request.setSourceAddr(sourceAddress);
            request.setShortMessage(shortMessage);
            request.setScheduleDeliveryTime(scheduleDeliveryTime);
            request.setValidityPeriod(validityPeriod);
            request.setRegisteredDelivery(registeredDelivery);
            request.setSmDefaultMsgId(smDefaultMsgId);

            // send the request
            LOGGER.fine("Replace request " + request.debugString());
            if (asynchronous) {
                session.replace(request);
            } else {
                response = session.replace(request);
                LOGGER.fine("Replace response " + response.debugString());
            }

        } catch (Exception e) {
            event.write(e, "");
            debug.write("Replace operation failed. " + e);
            LOGGER.fine("Replace operation failed. " + e);
        } finally {
            debug.exit(this);
        }
    }

    /**
     * Creates a new instance of <code>CancelSM</code> class, lets you set
     * subset of fields of it. This PDU is used to cancel an already submitted
     * message. You can only cancel a message which haven't been delivered to
     * the device yet.
     *
     * See "SMPP Protocol Specification 3.4, 4.9 CANCEL_SM Operation."
     *
     * @see Session#cancel(CancelSM)
     * @see CancelSM
     * @see CancelSMResp
     */
    private void cancel() {
        debug.enter(this, "SMPPTest.cancel()");
        try {
            CancelSM request = new CancelSM();
            CancelSMResp response;

            // input values
            serviceType = getParam("Service type", serviceType);
            messageId = getParam("Message id", messageId);
            sourceAddress = getAddress("Source", sourceAddress);
            destAddress = getAddress("Destination", destAddress);

            // set values
            request.setServiceType(serviceType);
            request.setMessageId(messageId);
            request.setSourceAddr(sourceAddress);
            request.setDestAddr(destAddress);

            // send the request
            LOGGER.fine("Cancel request " + request.debugString());
            if (asynchronous) {
                session.cancel(request);
            } else {
                response = session.cancel(request);
                LOGGER.fine("Cancel response " + response.debugString());
            }

        } catch (Exception e) {
            event.write(e, "");
            debug.write("Cancel operation failed. " + e);
            LOGGER.log(SEVERE, "Cancel operation failed. ", e);
        } finally {
            debug.exit(this);
        }
    }

    /**
     * Creates a new instance of <code>DataSM</code> class, lets you set subset
     * of fields of it. This PDU is an alternative to the <code>SubmitSM</code>
     * and
     * </code>DeliverSM</code>. It delivers the data to the specified device.
     *
     * See "SMPP Protocol Specification 3.4, 4.7 DATA_SM Operation."
     *
     * @see Session#data(DataSM)
     * @see DataSM
     * @see DataSMResp
     */
    private void data() {
        debug.enter(this, "SMPPTest.data()");

        try {
            DataSM request = new DataSM();
            DataSMResp response;

            // input values
            serviceType = getParam("Service type", serviceType);
            sourceAddress = getAddress("Source", sourceAddress, Data.SM_DATA_ADDR_LEN);
            destAddress = getAddress("Destination", destAddress, Data.SM_DATA_ADDR_LEN);
            esmClass = getParam("Esm class", esmClass);
            registeredDelivery = getParam("Registered delivery", registeredDelivery);
            dataCoding = getParam("Data encoding", dataCoding);

            // set values
            request.setServiceType(serviceType);
            request.setSourceAddr(sourceAddress);
            request.setDestAddr(destAddress);
            request.setEsmClass(esmClass);
            request.setRegisteredDelivery(registeredDelivery);
            request.setDataCoding(dataCoding);

            // send the request
            LOGGER.fine("Data request " + request.debugString());
            if (asynchronous) {
                session.data(request);
            } else {
                response = session.data(request);
                LOGGER.fine("Data response " + response.debugString());
                messageId = response.getMessageId();
            }

        } catch (Exception e) {
            event.write(e, "");
            debug.write("Data operation failed. " + e);
            LOGGER.log(SEVERE, "Data operation failed. ", e);
        } finally {
            debug.exit(this);
        }
    }

    /**
     * Creates a new instance of <code>QuerySM</code> class, lets you set subset
     * of fields of it. This PDU is used to fetch information about status of
     * already submitted message providing that you 'remember' message id of the
     * submitted message. The message id is assigned by SMSC and is returned to
     * you with the response to the submision PDU (SubmitSM, DataSM etc.).
     *
     * See "SMPP Protocol Specification 3.4, 4.8 QUERY_SM Operation."
     *
     * @see Session#query(QuerySM)
     * @see QuerySM
     * @see QuerySMResp
     */
    private void query() {
        debug.enter(this, "SMPPTest.query()");
        try {
            QuerySM request = new QuerySM();
            QuerySMResp response;

            // input values
            messageId = getParam("Message id", messageId);
            sourceAddress = getAddress("Source", sourceAddress);

            // set values
            request.setMessageId(messageId);
            request.setSourceAddr(sourceAddress);

            // send the request
            LOGGER.fine("Query request " + request.debugString());
            if (asynchronous) {
                session.query(request);
            } else {
                response = session.query(request);
                LOGGER.fine("Query response " + response.debugString());
                messageId = response.getMessageId();
            }

        } catch (Exception e) {
            event.write(e, "");
            debug.write("Query operation failed. " + e);
            LOGGER.log(SEVERE, "Query operation failed. ", e);
        } finally {
            debug.exit(this);
        }
    }

    /**
     * Creates a new instance of <code>EnquireSM</code> class. This PDU is used
     * to check that application level of the other party is alive. It can be
     * sent both by SMSC and ESME.
     *
     * See "SMPP Protocol Specification 3.4, 4.11 ENQUIRE_LINK Operation."
     *
     * @see Session#enquireLink(EnquireLink)
     * @see EnquireLink
     * @see EnquireLinkResp
     */
    private void enquireLink() {
        debug.enter(this, "SMPPTest.enquireLink()");
        try {

            EnquireLink request = new EnquireLink();
            EnquireLinkResp response;
            LOGGER.fine("Enquire Link request " + request.debugString());
            if (asynchronous) {
                session.enquireLink(request);
            } else {
                response = session.enquireLink(request);
                LOGGER.fine("Enquire Link response " + response.debugString());
            }
        } catch (IOException ioe) {
            bound = false;
            LOGGER.log(SEVERE, "Enquire Link operation failed. ", ioe);
        } catch (Exception e) {
            event.write(e, "");
            debug.write("Enquire Link operation failed. " + e);
            LOGGER.log(SEVERE, "Enquire Link operation failed. ", e);
        } finally {
            debug.exit(this);
        }
    }

    /**
     * Receives one PDU of any type from SMSC and prints it on the screen.
     *
     * @see Session#receive()
     * @see Response
     * @see ServerPDUEvent
     */
    private void receive() {
        debug.enter(this, "SMPPTest.receive()");
        try {

            PDU pdu = null;
            LOGGER.finest(() -> "Going to receive a PDU. ");
            if (receiveTimeout == Data.RECEIVE_BLOCKING) {
                LOGGER.fine(
                        "The receive is blocking, i.e. the application " + "will stop until a PDU will be received.");
            } else {
                LOGGER.finest(() -> "The receive timeout is " + receiveTimeout / 1000 + " sec.");
            }
            if (asynchronous) {
                ServerPDUEvent pduEvent = pduListener.getRequestEvent(receiveTimeout);
                if (pduEvent != null) {
                    pdu = pduEvent.getPDU();
                }
            } else {
                pdu = session.receive(receiveTimeout);
            }
            if (pdu != null) {
                LOGGER.fine("Received PDU " + pdu.debugString());
                if (pdu.isRequest()) {
                    Request request = (Request) pdu;
                    try {
                        final int commandId = request.getCommandId();
                        switch (commandId) {
                            case Data.ENQUIRE_LINK:
                                break;
                            case Data.DELIVER_SM:
                                DeliverSM deliver = (DeliverSM) request;
                                String receiptedMessageId = null;
                                String dlr = deliver.getShortMessage();
                                DLRInfo dlrInfo = getDLRInfo(dlr);
                                if (dlrInfo == null) {
                                    LOGGER.severe(String.format("Unable to confirm message based on DLR {%s}", dlr));
                                } else {
                                    if (deliver.hasReceiptedMessageId()) {
                                        receiptedMessageId = deliver.getReceiptedMessageId();
                                    } else {
                                        if ((receiptedMessageId = dlrInfo.getId()) == null) {
                                            LOGGER.warning(String.format("Message does not comply DLR format: {%s}", dlr));
                                        }
                                    }
                                    if (receiptedMessageId == null) {
                                        LOGGER.warning("Unable to get receipted message id (FS_ESME.CODE_SMSC)");
                                    } else {
                                        LOGGER.finest(String.format("CODE_SMSC should be {%s}", receiptedMessageId));
                                        OriginCode x = m_smsCodeFromSMSCID.remove(receiptedMessageId);
                                        try (java.sql.Connection conn = m_ds.getConnection()) {
                                            try {
                                                conn.setAutoCommit(false);
                                                StatEnum stat = dlrInfo.getStat();
                                                if (x == null) {
                                                    try (java.sql.PreparedStatement ps = conn.prepareStatement(
                                                            "select e.sms_cod, e.msgid_gw from fs_esme e where e.code_smsc = ? and e.code_ruta = ?")) {
                                                        ps.setString(1, receiptedMessageId);
                                                        ps.setInt(2, m_codeRuta);
                                                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                                                            if (rs.next()) {
                                                                x = new OriginCode(rs.getString("sms_cod"),
                                                                        rs.getString("msgid_gw"));
                                                                storeBasedOnStat(conn, x, receiptedMessageId, stat,
                                                                        dlrInfo.getDoneDate());
                                                            } else {
                                                                LOGGER.warning(String.format(
                                                                        "Unable to retrieve information for {%s}",
                                                                        receiptedMessageId));
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    storeBasedOnStat(conn, x, receiptedMessageId, stat,
                                                            dlrInfo.getDoneDate());
                                                }
                                                conn.commit();
                                            } catch (java.sql.SQLException e) {
                                                LOGGER.log(SEVERE, "Unexpected DB exception", e);
                                                try {
                                                    conn.rollback();
                                                } catch (java.sql.SQLException re) {
                                                    LOGGER.log(SEVERE, "Rollback", re);
                                                }
                                            } finally {
                                                conn.setAutoCommit(true);
                                            }
                                        }
                                    }
                                }
                                break;
                            case Data.SUBMIT_SM:// Reply
                                break;
                            case Data.UNBIND:
                                exit();
                                break;
                            default:
                                LOGGER.warning(String.format("Unhandled command {%s}", Integer.toHexString(commandId)));
                                break;
                        }
                    } finally {
                        Response response = request.getResponse();
                        // respond with default response
                        LOGGER.fine("Going to send default response to request " + response.debugString());
                        session.respond(response);
                    }
                }
            } else {
                LOGGER.finest("No PDU received this time.");
            }

        } catch (Exception e) {
            event.write(e, "");
            debug.write("Receiving failed. " + e);
            LOGGER.log(SEVERE, "Receiving failed. ", e);
        } finally {
            debug.exit(this);
        }
    }

    protected void storeBasedOnStat(java.sql.Connection conn, OriginCode x, String receiptedMessageId, StatEnum stat,
            String doneDate) throws SQLException {
        if (stat == null) {
            LOGGER.severe(String.format("Empty stat for {%s}", receiptedMessageId));
        } else {
            LOGGER.fine(String.format("Stat for {%s} was {%s}", receiptedMessageId, stat));
            switch (stat) {
                case ACCEPTD:
                    break;
                case DELIVRD:
                    store(conn, x, receiptedMessageId, doneDate);
                    break;
                case UNKNOWN:
                case EXPIRED:
                case DELETED:
                case UNDELIV:
                case REJECTD:
                    markError(conn, x.getSmsCode(), x.getMsgIdGW());
                    break;
                default:
                    LOGGER.severe(String.format("Unexpected DLR.stat for {%s} was {%s}", receiptedMessageId, stat));
                    break;
            }
        }
    }

    private void store(java.sql.Connection conn, OriginCode x, String receiptedMessageId, String doneDate)
            throws SQLException {
        
        //LOGGER.warning("En SMPPClient: insert into FS_ESME_DATA (select e.sms_cod, e.dst_addrs, e.sms_txt, e.fecha_hora, e.code_ruta, e.code_smsc, 0 , TO_DATE("+doneDate+", 'YYMMDDHH24MI'), e.sourc_addrs from fs_esme e where e.code_smsc = "+receiptedMessageId+" and e.code_ruta = "+m_codeRuta+" )");
        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                "insert into FS_ESME_DATA (select e.sms_cod, e.dst_addrs, e.sms_txt, e.fecha_hora, e.code_ruta, e.code_smsc, 0 /*NO ERROR (legacy)*/, TO_DATE(?, 'YYMMDDHH24MI'), e.sourc_addrs from fs_esme e where e.code_smsc = ? and e.code_ruta = ? )")) {
            ps.setString(1, doneDate);
            ps.setString(2, receiptedMessageId);
            ps.setInt(3, m_codeRuta);
            int rowCount = ps.executeUpdate();
            assert rowCount > 0;
        }
        try (java.sql.PreparedStatement ps = conn.prepareStatement("DELETE FROM FS_ESME WHERE SMS_COD=? ")) {
            ps.setString(1, x.getSmsCode());
            int rowCount = ps.executeUpdate();
            assert rowCount > 0;
        }
        if (x.getMsgIdGW() == null) {
            // TODO Handle
        } else {
            try (java.sql.PreparedStatement ps = conn
                    .prepareStatement("UPDATE FS_SMSCGW SET ID_REPORT=? WHERE MSGID=?")) {
                ps.setString(1, DAO.ReportState.OK.getState());
                ps.setString(2, x.getMsgIdGW());
                int rowCount = ps.executeUpdate();
                assert rowCount > 0;
            }
        }
    }

    /**
     * If bound, unbinds and then exits this application.
     */
    private void exit() {
        debug.enter(this, "SMPPTest.exit()");
        if (bound) {
            unbind();
        }
        keepRunning = false;
        debug.exit(this);
    }

    /**
     * Implements simple PDU listener which handles PDUs received from SMSC. It
     * puts the received requests into a queue and discards all received
     * responses. Requests then can be fetched (should be) from the queue by
     * calling to the method <code>getRequestEvent</code>.
     *
     * @see Queue
     * @see ServerPDUEvent
     * @see ServerPDUEventListener
     * @see SmppObject
     */
    private class SMPPTestPDUEventListener extends SmppObject implements ServerPDUEventListener {

        Session session;
        Queue requestEvents = new Queue();

        public SMPPTestPDUEventListener(Session session) {
            this.session = session;
        }

        public void handleEvent(ServerPDUEvent event) {
            PDU pdu = event.getPDU();
            if (pdu.isRequest()) {
                LOGGER.finest(() -> "async request received, enqueuing " + pdu.debugString());
                synchronized (requestEvents) {
                    requestEvents.enqueue(event);
                    requestEvents.notify();
                }
            } else if (pdu.isResponse()) {
                LOGGER.finest(() -> "async response received " + pdu.debugString());
                if (Data.SUBMIT_SM_RESP == pdu.getCommandId()) {
                    SubmitSMResp submitSMResp = (SubmitSMResp) pdu;
                    final int seqN = submitSMResp.getSequenceNumber();
                    OriginCode x = m_smsCodeFromSequenceMap.remove(seqN);
                    if (x == null) {
                        LOGGER.warning("Missing sequence number for: " + pdu.debugString());
                    } else {
                        final String smsCode = x.getSmsCode();
                        if (smsCode == null) {
                            LOGGER.warning(String.format("SMS code for {%d} is missing", seqN));
                        } else {
                            try (java.sql.Connection conn = m_ds.getConnection()) {
                                try {
                                    conn.setAutoCommit(false);
                                    final int commandStatus = submitSMResp.getCommandStatus();
                                    if (commandStatus > 0) { // Error
                                        LOGGER.warning(String.format("Submit {%s}, sequence {%d} failed with error %d",
                                                smsCode, seqN, commandStatus));
                                        markError(conn, smsCode, x.getMsgIdGW());
                                        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                                                "UPDATE FS_SMS_TELES SET FAILED_COUNT=FAILED_COUNT+1 WHERE RUTA_COD=?")) {
                                            ps.setInt(1, m_codeRuta);
                                            int rowCount = ps.executeUpdate();
                                            assert rowCount > 0;
                                        }
                                    } else {
                                        final String messageId = submitSMResp.getMessageId();
                                        m_smsCodeFromSMSCID.put(messageId, x);
                                        LOGGER.info(String.format("*********** %d -> %s {%s} took {%s}", seqN, smsCode,
                                                messageId, x.getElapsedTime()));
                                        try (java.sql.PreparedStatement ps = conn
                                                .prepareStatement("UPDATE FS_ESME SET CODE_SMSC=? where SMS_COD=?")) {
                                            ps.setString(1, messageId);
                                            ps.setString(2, smsCode);
                                            int rowCount = ps.executeUpdate();
                                            assert rowCount > 0;
                                        }
                                        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                                                "UPDATE FS_SMS_TELES SET DELIVERED_COUNT=DELIVERED_COUNT+1 WHERE RUTA_COD=?")) {
                                            ps.setInt(1, m_codeRuta);
                                            int rowCount = ps.executeUpdate();
                                            assert rowCount > 0;
                                        }
                                    }
                                    conn.commit();
                                } catch (java.sql.SQLException e) {
                                    try {
                                        conn.rollback();
                                    } catch (java.sql.SQLException re) {
                                        LOGGER.log(SEVERE, "Rollback", re);
                                    }
                                    throw e;
                                } finally {
                                    conn.setAutoCommit(true);
                                }
                            } catch (java.sql.SQLException e) {
                                LOGGER.log(SEVERE, "Error trying to match sequence and sms code", e);
                            }
                        }
                    }
                }
            } else {
                LOGGER.warning("pdu of unknown class (not request nor " + "response) received, discarding "
                        + pdu.debugString());
            }
        }

        /**
         * Returns received pdu from the queue. If the queue is empty, the
         * method blocks for the specified timeout.
         */
        public ServerPDUEvent getRequestEvent(long timeout) throws InterruptedException {
            ServerPDUEvent pduEvent = null;
            synchronized (requestEvents) {
                while (requestEvents.isEmpty() && session.isOpened()) {
                    requestEvents.wait(timeout);
                }
                pduEvent = (ServerPDUEvent) requestEvents.dequeue();
            }
            return pduEvent;
        }
    }

    /**
     * Prompts the user to enter a string value for a parameter.
     */
    private String getParam(String prompt, String defaultValue) {
        return defaultValue;
    }

    /**
     * Prompts the user to enter a byte value for a parameter.
     */
    private byte getParam(String prompt, byte defaultValue) {
        return Byte.parseByte(getParam(prompt, Byte.toString(defaultValue)));
    }

    /**
     * Prompts the user to enter an integer value for a parameter.
     */
    private int getParam(String prompt, int defaultValue) {
        return Integer.parseInt(getParam(prompt, Integer.toString(defaultValue)));
    }

    /**
     * Prompts the user to enter an address value with specified max length.
     */
    private Address getAddress(String type, Address address, int maxAddressLength) throws WrongLengthOfStringException {
        byte ton = getParam(type + " address TON", address.getTon());
        byte npi = getParam(type + " address NPI", address.getNpi());
        String addr = getParam(type + " address", address.getAddress());
        address.setTon(ton);
        address.setNpi(npi);
        address.setAddress(addr, maxAddressLength);
        return address;
    }

    /**
     * Prompts the user to enter an address value with max length set to the
     * default length Data.SM_ADDR_LEN.
     */
    private Address getAddress(String type, Address address) throws WrongLengthOfStringException {
        return getAddress(type, address, Data.SM_ADDR_LEN);
    }

    /**
     * Loads configuration parameters from the file with the given name. Sets
     * private variable to the loaded values.
     */
    private void loadProperties(String fileName) throws IOException {
        LOGGER.config("Reading configuration file " + fileName + "...");
        FileInputStream propsFile = new FileInputStream(fileName);
        properties.load(propsFile);
        propsFile.close();
        LOGGER.config("Setting default parameters...");
        byte ton;
        byte npi;
        String addr;
        String bindMode;
        int rcvTimeout;
        String syncMode;

        ipAddress = properties.getProperty("ip-address");
        port = getIntProperty("port", port);
        systemId = properties.getProperty("system-id");
        password = properties.getProperty("password");

        ton = getByteProperty("addr-ton", addressRange.getTon());
        npi = getByteProperty("addr-npi", addressRange.getNpi());
        addr = properties.getProperty("address-range", addressRange.getAddressRange());
        addressRange.setTon(ton);
        addressRange.setNpi(npi);
        try {
            addressRange.setAddressRange(addr);
        } catch (WrongLengthOfStringException e) {
            LOGGER.log(WARNING, "The length of address-range parameter is wrong.", e);
        }

        ton = getByteProperty("source-ton", sourceAddress.getTon());
        npi = getByteProperty("source-npi", sourceAddress.getNpi());
        addr = properties.getProperty("source-address", sourceAddress.getAddress());
        setAddressParameter("source-address", sourceAddress, ton, npi, addr);

        ton = getByteProperty("destination-ton", destAddress.getTon());
        npi = getByteProperty("destination-npi", destAddress.getNpi());
        addr = properties.getProperty("destination-address", destAddress.getAddress());
        setAddressParameter("destination-address", destAddress, ton, npi, addr);

        serviceType = properties.getProperty("service-type", serviceType);
        systemType = properties.getProperty("system-type", systemType);
        bindMode = properties.getProperty("bind-mode", bindOption);
        if (bindMode.equalsIgnoreCase("transmitter")) {
            bindMode = "t";
        } else if (bindMode.equalsIgnoreCase("receiver")) {
            bindMode = "r";
        } else if (bindMode.equalsIgnoreCase("transciever")) {
            bindMode = "tr";
        } else if (!bindMode.equalsIgnoreCase("t") && !bindMode.equalsIgnoreCase("r")
                && !bindMode.equalsIgnoreCase("tr")) {
            LOGGER.warning("The value of bind-mode parameter in " + "the configuration file " + fileName + " is wrong. "
                    + "Setting the default");
            bindMode = "t";
        }
        bindOption = bindMode;

        // receive timeout in the cfg file is in seconds, we need milliseconds
        // also conversion from -1 which indicates infinite blocking
        // in the cfg file to Data.RECEIVE_BLOCKING which indicates infinite
        // blocking in the library is needed.
        if (receiveTimeout == Data.RECEIVE_BLOCKING) {
            rcvTimeout = -1;
        } else {
            rcvTimeout = ((int) receiveTimeout) / 1000;
        }
        rcvTimeout = getIntProperty("receive-timeout", rcvTimeout);
        if (rcvTimeout == -1) {
            receiveTimeout = Data.RECEIVE_BLOCKING;
        } else {
            receiveTimeout = rcvTimeout * 1000;
        }

        syncMode = properties.getProperty("sync-mode", (asynchronous ? "async" : "sync"));
        if (syncMode.equalsIgnoreCase("sync")) {
            asynchronous = false;
        } else if (syncMode.equalsIgnoreCase("async")) {
            asynchronous = true;
        } else {
            asynchronous = false;
        }

        /*
		 * scheduleDeliveryTime validityPeriod shortMessage numberOfDestination
		 * messageId esmClass protocolId priorityFlag registeredDelivery
		 * replaceIfPresentFlag dataCoding smDefaultMsgId
         */
    }

    /**
     * Gets a property and converts it into byte.
     */
    private byte getByteProperty(String propName, byte defaultValue) {
        return Byte.parseByte(properties.getProperty(propName, Byte.toString(defaultValue)));
    }

    /**
     * Gets a property and converts it into integer.
     */
    private int getIntProperty(String propName, int defaultValue) {
        return Integer.parseInt(properties.getProperty(propName, Integer.toString(defaultValue)));
    }

    /**
     * Sets attributes of <code>Address</code> to the provided values.
     */
    private void setAddressParameter(String descr, Address address, byte ton, byte npi, String addr) {
        address.setTon(ton);
        address.setNpi(npi);
        try {
            address.setAddress(addr);
        } catch (WrongLengthOfStringException e) {
            LOGGER.log(WARNING, "The length of " + descr + " parameter is wrong.", e);
        }
    }

    public static DLRInfo getDLRInfo(final String dlr) {
        DLRInfo ret = null;
        if (dlr == null) {
            LOGGER.warning(() -> "Empty DLR");
        } else {
            final String lcDLR = dlr.toLowerCase();
            //int end = lcDLR.indexOf(" text:");
            int end = lcDLR.indexOf(" err:");
            if (end < 0) {
                LOGGER.warning(() -> String.format("Malformed DLR: {%s}", dlr));
            } //else {
                String input = lcDLR.substring(0, end+6);
                // id:1525f3d3 sub:001 dlvrd:001 submit date:2207111739 done date:2207111739 stat:DELIVRD err:000 text:Hola X $ cristian na
                final String pattern = "^id:(\\S+) sub:(\\S+) dlvrd:(\\S+) submit date:(\\S+) done date:(\\S+) stat:(\\S+) err:(\\S+)$";
                Matcher m = Pattern.compile(pattern).matcher(input);
                if (m.matches()) {
                    ret = new DLRInfo();
                    switch (m.groupCount()) {
                        case 7:
                            //LOGGER.warning(() -> String.format("--> m.group(7): {%s}", m.group(7)));
                            ret.setErr(m.group(7));
                        case 6:
                            //LOGGER.warning(() -> String.format("--> m.group(6): {%s}", m.group(6)));
                            ret.setStat(StatEnum.valueOf(m.group(6).toUpperCase()));
                        case 5:
                            ret.setDoneDate(m.group(5));
                        case 4:
                            ret.setSubmitDate(m.group(4));
                        case 3:
                            ret.setDlvrd(m.group(3));
                        case 2:
                            ret.setSub(m.group(2));
                        case 1:
                            ret.setId(m.group(1));
                        default:
                            break;
                    }
                } else {
                    LOGGER.warning(
                            String.format("Segmented DLR {%s} does not match pattern {%s}", input, pattern.toString()));
                }
            //}
        }
        return ret;
    }
}
