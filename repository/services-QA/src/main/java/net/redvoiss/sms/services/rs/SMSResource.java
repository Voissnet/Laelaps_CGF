package net.redvoiss.sms.services.rs;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.io.IOException;
import java.sql.SQLException;
import java.io.FileNotFoundException;
import javax.annotation.Resource;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import net.redvoiss.sms.services.SMSService;
import net.redvoiss.sms.services.bean.BatchReply;
import net.redvoiss.sms.services.bean.BatchStatus;
import net.redvoiss.sms.services.bean.BulkMessage;
import net.redvoiss.sms.services.bean.MessageReply;
import net.redvoiss.sms.services.bean.MessageStatus;
import net.redvoiss.sms.services.error.SMSError;

 
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.regex.*; 

import java.security.MessageDigest;
import org.mindrot.jbcrypt.BCrypt;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Path("/")
@javax.enterprise.context.RequestScoped
public class SMSResource implements SMSService {
    private static final Logger LOGGER = Logger.getLogger(SMSResource.class.getName());

    @Resource(lookup = "jdbc/DB_DESA")
    private DataSource m_dataSource;

    @Path("greet") // e.g. 'curl -u joavila -i -X GET https://sms.lanube.cl/services/rest/greet' or
                   // 'curl -u joavila -i -X GET https://sms.lanube.cl:8181/services/rest/greet'
    @GET
    public String doGreet(@Context SecurityContext sc) throws SQLException {
        String username = sc.getUserPrincipal().getName() == null ? "Stranger" : sc.getUserPrincipal().getName();

        StringBuffer msg = new StringBuffer(String.format("Hello %s!", username));
        try (Connection conn = m_dataSource.getConnection()) {
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT SYSDATE FROM DUAL")) {
                if (rs.next()) {
                    msg.append(String.format(" Local time is %s", String.valueOf(rs.getTimestamp(1))));
                }
            }
        }
        return msg.toString();
    }

    @GET
    @Path("initsms") 
    public String doInitSms(@Context SecurityContext sc) throws SQLException , FileNotFoundException ,IOException {
        String username = sc.getUserPrincipal().getName() == null ? "Stranger" : sc.getUserPrincipal().getName();
        StringBuffer msg = new StringBuffer("Procesando peticion ");

        String appreinit = System.getProperty("appreinit");
        String cmdLine = ""; 
        String appname = "";
        
        LOGGER.log(WARNING,"Inicio llamada a servlet 'initsms' con appreinit="+appname+", USERNAME: "+username);
        
        if ("true".equals(appreinit))
        {
            File file = new File("/opt/glassfish/glassfish-4.1.2/glassfish4/glassfish/nodes/localhost-redvoiss/reinit.cfg"); 
            try 
            {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String st = "";
                if ((st = br.readLine()) != null)
                {
                    String[] vals=st.split("=");
                    appname = vals[1];
                    if ((st = br.readLine()) != null)
                    {
                        st=st.replace("cmdline=","");
                        cmdLine = st;
                    }
                }
                LOGGER.log(WARNING,"appReinit="+appname+", cmdLine:" + cmdLine);
            } catch (IOException e) {
                LOGGER.log(WARNING,"IOException opening appName_reinit file:" + e.getMessage());
            //} catch (FileNotFoundException e) {
            //    LOGGER.log(WARNING,"Exception opening appName_reinit file:" + e.getMessage());
            }

        
            try {
            
            String[] cmd = { "/bin/sh", "-c", "ps -fea | grep '"+appname+"'"};
            Process p = Runtime.getRuntime().exec(cmd);
            try(BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;                                       
                while ((line = input.readLine()) != null) 
                {
                    if (line.indexOf("AdminMain")>0)
                    {
                        Pattern pattern = Pattern.compile("(\\w+)(\\s+)(\\d+)");
                        Matcher matcher = pattern.matcher(line);
                        String[] cmdReinit = { "/bin/sh", "-c", cmdLine };
                        String key = "";
                        String value = "";
                        if (matcher.find()) {
                            key = matcher.group(1);
                            value = matcher.group(3);
                            msg.append("PID : " + value );
                                
                            LOGGER.log(WARNING,"MATARE proceso PID:" + value+", CMD: "+line);
                            Process p2 = Runtime.getRuntime().exec("kill -9 "+value);
                            LOGGER.log(WARNING,"MATE proceso "+value+", y esperare 5 SEGUNDOS");
                                                       
                            try
                            {
                                Thread.sleep(5000);
                            }
                            catch(InterruptedException ex)
                            {
                                Thread.currentThread().interrupt();
                            }
                            
                            LOGGER.log(WARNING,"Reiniciando proceso DIGEVO");
                            p2.destroy();

                            Process p3 = Runtime.getRuntime().exec(cmdReinit);
                            LOGGER.log(WARNING,"Proceso "+appname+" reiniciado :");                                                        
                            p3. waitFor();
                            
                            try {
                                BufferedReader input2 = new BufferedReader(new InputStreamReader(p3.getInputStream()));
                                String line2 = "";
                                while ((line2=input2.readLine())!=null) 
                                    LOGGER.log(WARNING,line2);
                            } catch (Exception err) {
                                err.printStackTrace();
                            }               
                            p3.destroy();
                        }       
                    }
                }
                p.destroy();
            }
            } catch (Exception err) {
                err.printStackTrace();
            }        
        }
   
        return msg.toString();
    }
    
    @GET
    @Path("/batch/{batchId : \\d+}/status")
    @Produces(APPLICATION_JSON)
    public Response getBatchStatus(@Context SecurityContext sc, @PathParam("batchId") int batchId) throws Exception {
        String username = sc.getUserPrincipal().getName() == null ? "Stranger" : sc.getUserPrincipal().getName();
        try {
            List<BatchStatus> batchStatusList = checkBatchStatus(batchId, username, m_dataSource);
            if (batchStatusList.isEmpty()) {
                return Response.noContent().build();
            }
            return Response.ok().type(APPLICATION_JSON).entity(batchStatusList).build();
        } catch (SMSError e) {
            LOGGER.log(SEVERE, e, () -> String.format("Exception while getting status for batch {%s}", batchId));
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @GET
    @Path("/batch/{batchId : \\d+}/replies")
    @Produces(APPLICATION_JSON)
    public Response getBatchReplies(@Context SecurityContext sc, @PathParam("batchId") int batchId) throws Exception {
        String username = sc.getUserPrincipal().getName() == null ? "Stranger" : sc.getUserPrincipal().getName();
        try {
            BatchReply batchReply = checkBatchReplies(batchId, username, m_dataSource);
            return Response.ok().type(APPLICATION_JSON).entity(batchReply).build();
        } catch (SMSError e) {
            LOGGER.log(SEVERE, e, () -> String.format("Exception while getting status for batch {%s}", batchId));
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @GET
    @Path("/message/{messageId : \\d+}/status")
    @Produces(APPLICATION_JSON)
    public Response getMessageStatus(@Context SecurityContext sc, @PathParam("messageId") int messageId)
            throws Exception {
        String username = sc.getUserPrincipal().getName() == null ? "Stranger" : sc.getUserPrincipal().getName();
        try {
            MessageStatus status = checkMessageStatus(messageId, username, m_dataSource);
            if (status == null) {
                return Response.noContent().build();
            }
            return Response.ok().type(APPLICATION_JSON).entity(status).build();
        } catch (SMSError e) {
            LOGGER.log(SEVERE, e, () -> String.format("Exception while gettime message status for {%s}", messageId));
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @GET
    @Path("/message/{messageId : \\d+}/replies")
    @Produces(APPLICATION_JSON)
    public Response getMessageReply(@Context SecurityContext sc, @PathParam("messageId") int messageId)
            throws Exception {
        String username = sc.getUserPrincipal().getName() == null ? "Stranger" : sc.getUserPrincipal().getName();
        try {
            List<MessageReply> messageReplyList = checkMessageReply(messageId, username, m_dataSource);
            if (messageReplyList.isEmpty()) {
                return Response.noContent().build();
            }
            return Response.ok().type(APPLICATION_JSON).entity(messageReplyList).build();
        } catch (SMSError e) {
            LOGGER.log(SEVERE, e, () -> String.format("Exception while getting reply for message {%s}", messageId));
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    
    @POST
    @Path("send")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Transactional(rollbackOn = { SQLException.class })
    public Response sendMessage(@Context SecurityContext sc, BulkMessage bulkMessage) throws SQLException, SMSError {
        final String username = sc.getUserPrincipal().getName() == null ? "Stranger" : sc.getUserPrincipal().getName();
        Map<String, String> m = new HashMap<String, String>();
        
        
        //validaPassword("secreta1975", "8937eaccac00b4fa1e9d70531b9af0cb6e425e8d8e0dc934db7355534334b8b1");

        m.put("batchId", send(username, bulkMessage, m_dataSource, false));
        return Response.ok().type(APPLICATION_JSON).entity(m).build();
    }

    @POST
    @Path("sendWithReply")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Transactional(rollbackOn = { SQLException.class })
    public Response sendMessageWithReply(@Context SecurityContext sc, BulkMessage bulkMessage)
            throws SQLException, SMSError {
        final String username = sc.getUserPrincipal().getName() == null ? "Stranger" : sc.getUserPrincipal().getName();
        Map<String, String> m = new HashMap<String, String>();
        m.put("batchId", send(username, bulkMessage, m_dataSource, true));
        return Response.ok().type(APPLICATION_JSON).entity(m).build();
    }
    
    @POST
    @Path("send_v2")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Transactional(rollbackOn = { SQLException.class })
    public Response sendMessage_v2(@Context SecurityContext sc, BulkMessage bulkMessage) throws SQLException, SMSError {
        final String username = sc.getUserPrincipal().getName() == null ? "Stranger" : sc.getUserPrincipal().getName();
        Map<String, String> m = new HashMap<String, String>();
        
        /* CGF, 20200911. I add to JSON output to include destinations that failed validation */
        String resu = send_v2(username, bulkMessage, m_dataSource, false);
        LOGGER.info(String.format("send_v2() call returned {%s} en sendMessage_v2()", resu));
        String[] retorno = resu.split("[|]");
        if (retorno.length>1)
        {
            m.put("batchId", retorno[0]);
            m.put("failedIds", retorno[1]);
        } else {
            m.put("batchId", retorno[0]);
            m.put("failedIds", "-1");
        }        
        /* CGF, 20200911. Fin */
        return Response.ok().type(APPLICATION_JSON).entity(m).build();
    }

    @POST
    @Path("sendWithReply_v2")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Transactional(rollbackOn = { SQLException.class })
    public Response sendMessageWithReply_v2(@Context SecurityContext sc, BulkMessage bulkMessage)
            throws SQLException, SMSError {
        final String username = sc.getUserPrincipal().getName() == null ? "Stranger" : sc.getUserPrincipal().getName();
        Map<String, String> m = new HashMap<String, String>();
        /* CGF, 20200911. I add to JSON output to include destinations that failed validation */
        String resu = send_v2(username, bulkMessage, m_dataSource, true);
        LOGGER.info(String.format("send_v2() call returned {%s} en sendMessageWithReply_v2()", resu));
        String[] retorno = resu.split("[|]");
        if (retorno.length>1)
        {
            m.put("batchId", retorno[0]);
            m.put("failedIds", retorno[1]);
        } else {
            m.put("batchId", retorno[0]);
            m.put("failedIds", "-1");
        }
        /* CGF, 20200911. Fin */
        return Response.ok().type(APPLICATION_JSON).entity(m).build();
    }
    
    @POST
    @Path("send_v3")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Transactional(rollbackOn = { SQLException.class })
    public Response sendMessage_v3(@Context SecurityContext sc, BulkMessage bulkMessage) throws SQLException, SMSError {
        final String username = sc.getUserPrincipal().getName() == null ? "Stranger" : sc.getUserPrincipal().getName();
        Map<String, String> m = new HashMap<String, String>();
        
        /* CGF, 20200911. I add to JSON output to include destinations that failed validation */
        String resu = send_v3(username, bulkMessage, m_dataSource, false);
        LOGGER.info(String.format("send_v3() call returned {%s} en sendMessage_v3()", resu));
        String[] retorno = resu.split("[|]");
        if (retorno.length>1)
        {
            m.put("batchId", retorno[0]);
            m.put("failedIds", retorno[1]);
        } else {
            m.put("batchId", retorno[0]);
            m.put("failedIds", "-1");
        }        
        /* CGF, 20200911. Fin */
        return Response.ok().type(APPLICATION_JSON).entity(m).build();
    }

    
    private static final char[] hex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static void validaPassword(String passWS, String passDB) {
        try {
            String semilla = "Anomander_Rake"; 

            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(semilla.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            //String hash = byteArray2Hex( sha256_HMAC.doFinal(passDB.getBytes()) );
            String cryptedPassDB = BCrypt.hashpw(passDB, BCrypt.gensalt());
            LOGGER.warning("cryptedPassDB="+cryptedPassDB);
            
            String hashPassWS = byteArray2Hex( sha256_HMAC.doFinal(passWS.getBytes()) );
            LOGGER.warning("hashPassWS="+hashPassWS);

            String cryptedPassBD = BCrypt.hashpw(hashPassWS, BCrypt.gensalt());

            if (BCrypt.checkpw(hashPassWS, cryptedPassBD)) {
                LOGGER.warning("Password is OK");
            } else {
                LOGGER.warning("Password is INVALID");

                //System.out.println("It doesn't matches");
            }
        } catch (Exception e){
           //System.out.println("Error");
        }
    }

    static String byteArray2Hex(byte[] bytes) {
       StringBuffer sb = new StringBuffer(bytes.length * 2);
       for(final byte b : bytes) {
           sb.append(hex[(b & 0xF0) >> 4]);
           sb.append(hex[b & 0x0F]);
        }
        return sb.toString();
    }
        
}