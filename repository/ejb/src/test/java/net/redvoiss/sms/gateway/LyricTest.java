package net.redvoiss.sms.gateway;

import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class LyricTest {

    @Test(expected = javax.json.stream.JsonParsingException.class)
    public void testEmptyJSONMessage() throws Exception {
        javax.json.JsonReader jsonReader = javax.json.Json.createReader(new java.io.StringReader(""));
        javax.json.JsonObject json = jsonReader.readObject();
    }

    @Test
    public void testParseOutgoingMessageStatusPending() throws Exception {
        final String message = "{\"success\":true,\"message_status\":0,\"num\":\"+56448909433\",\"channel\":0,\"last_error\":0,\"n_tries\":0,\"delivery_status\":0,\"report_stage\":0,\"send_date\":\"0\",\"recv_date\":\"1452279058\",\"delivery_date\":\"0\"}";
        javax.json.JsonReader jsonReader = javax.json.Json.createReader(new java.io.StringReader(message));
        javax.json.JsonObject json = jsonReader.readObject();
        assertTrue(json.getBoolean("success"));
        assertEquals(0, json.getInt("message_status"));
    }

    @Test
    public void testParseOutgoingMessageStatusDelivered() throws Exception {
        final String message = "{\"success\":true,\"message_status\":2,\"num\":\"+56448909433\",\"channel\":4,\"last_error\":0,\"n_tries\":0,\"delivery_status\":0,\"report_stage\":2,\"send_date\":\"1452278489\",\"recv_date\":\"1452278462\",\"delivery_date\":\"1452278373\"}";
        javax.json.JsonReader jsonReader = javax.json.Json.createReader(new java.io.StringReader(message));
        javax.json.JsonObject json = jsonReader.readObject();
        assertTrue(json.getBoolean("success"));
        assertEquals(2, json.getInt("message_status"));
    }

    @Test
    public void testParseApiGetChannelsStatus() throws Exception {
        {// Usual
            final String message = "{ \"channels\": [ { \"id\": 1, \"sms_send_ena\": 1, \"state\": \"retrying_sms\", \"imsi\": \"730030700017561\", \"n_sent_sms\": 0 }, { \"id\": 2, \"sms_send_ena\": 1, \"state\": \"sending_sms\", \"imsi\": \"730030210913396\", \"n_sent_sms\": 0 }, { \"id\": 3, \"sms_send_ena\": 1, \"state\": \"sending_sms\", \"imsi\": \"730030402051046\", \"n_sent_sms\": 0 }, { \"id\": 4, \"sms_send_ena\": 1, \"state\": \"sending_sms\", \"imsi\": \"730030400775991\", \"n_sent_sms\": 0 }, { \"id\": 5, \"sms_send_ena\": 1, \"state\": \"sending_sms\", \"imsi\": \"730030402091882\", \"n_sent_sms\": 0 }, { \"id\": 6, \"sms_send_ena\": 1, \"state\": \"retrying_sms\", \"imsi\": \"730030217445672\", \"n_sent_sms\": 0 } ], \"success\": true } ";
            javax.json.JsonReader jsonReader = javax.json.Json.createReader(new java.io.StringReader(message));
            javax.json.JsonObject json = jsonReader.readObject();
            assertTrue(json.getBoolean("success"));
            javax.json.JsonArray arr = json.getJsonArray("channels");
            for (int i = 0; i < arr.size(); i++) {
                javax.json.JsonObject o = arr.getJsonObject(i);
                final String state = o.getString("state");
                assertNotNull(state);
            }
        }
        {// no_simcard
            final String message = "{ \"channels\": [ { \"id\": 1, \"sms_send_ena\": 1, \"state\": \"sending_sms\", \"imsi\": \"730030217445670\", \"n_sent_sms\": 0 }, { \"id\": 2, \"sms_send_ena\": 1, \"state\": \"no_simcard\" }, { \"id\": 3, \"sms_send_ena\": 1, \"state\": \"sending_sms\", \"imsi\": \"730030217659176\", \"n_sent_sms\": 0 }, { \"id\": 4, \"sms_send_ena\": 1, \"state\": \"sending_sms\", \"imsi\": \"730030400437183\", \"n_sent_sms\": 0 }, { \"id\": 5, \"sms_send_ena\": 1, \"state\": \"sending_sms\", \"imsi\": \"730030400522863\", \"n_sent_sms\": 0 }, { \"id\": 6, \"sms_send_ena\": 1, \"state\": \"sending_sms\", \"imsi\": \"730030210913080\", \"n_sent_sms\": 0 }, { \"id\": 7, \"sms_send_ena\": 1, \"state\": \"sending_sms\", \"imsi\": \"730030210913582\", \"n_sent_sms\": 0 }, { \"id\": 8, \"sms_send_ena\": 1, \"state\": \"sending_sms\", \"imsi\": \"730030210888506\", \"n_sent_sms\": 0 } ], \"success\": true }";
            javax.json.JsonReader jsonReader = javax.json.Json.createReader(new java.io.StringReader(message));
            javax.json.JsonObject json = jsonReader.readObject();
            assertTrue(json.getBoolean("success"));
            javax.json.JsonArray arr = json.getJsonArray("channels");
            javax.json.JsonObject o = arr.getJsonObject(1);
            final String state = o.getString("state");
            assertNotNull(state);
            assertEquals("no_simcard", state);
        }
        {// initiating
            final String message = "{ \"channels\": [ { \"id\": 1, \"sms_send_ena\": 1, \"state\": \"sending_sms\", \"imsi\": \"730030210887522\", \"n_sent_sms\": 0 }, { \"id\": 2, \"sms_send_ena\": 1, \"state\": \"retrying_sms\", \"imsi\": \"730030210886964\", \"n_sent_sms\": 0 }, { \"id\": 3, \"sms_send_ena\": 0, \"state\": \"initiating\" }, { \"id\": 4, \"sms_send_ena\": 0, \"state\": \"initiating\" }, { \"id\": 5, \"sms_send_ena\": 1, \"state\": \"initiating\" }, { \"id\": 6, \"sms_send_ena\": 1, \"state\": \"sending_sms\", \"imsi\": \"730030400775937\", \"n_sent_sms\": 0 }, { \"id\": 7, \"sms_send_ena\": 1, \"state\": \"sending_sms\", \"imsi\": \"730030210888066\", \"n_sent_sms\": 0 }, { \"id\": 8, \"sms_send_ena\": 1, \"state\": \"sending_sms\", \"imsi\": \"730030402051240\", \"n_sent_sms\": 0 } ], \"success\": true }";
            javax.json.JsonReader jsonReader = javax.json.Json.createReader(new java.io.StringReader(message));
            javax.json.JsonObject json = jsonReader.readObject();
            assertTrue(json.getBoolean("success"));
            javax.json.JsonArray arr = json.getJsonArray("channels");
            javax.json.JsonObject o = arr.getJsonObject(3);
            final String state = o.getString("state");
            assertNotNull(state);
            assertEquals("initiating", state);
        }
        {// registered
            final String message = "{ \"channels\": [ { \"id\": 1, \"sms_send_ena\": 1, \"state\": \"sending_sms\", \"imsi\": \"730030217445670\", \"n_sent_sms\": 0 }, { \"id\": 2, \"sms_send_ena\": 1, \"state\": \"no_simcard\" }, { \"id\": 3, \"sms_send_ena\": 1, \"state\": \"registered\", \"imsi\": \"730030217659176\", \"n_sent_sms\": 0 }, { \"id\": 4, \"sms_send_ena\": 1, \"state\": \"registered\", \"imsi\": \"730030400437183\", \"n_sent_sms\": 0 }, { \"id\": 5, \"sms_send_ena\": 1, \"state\": \"registered\", \"imsi\": \"730030400522863\", \"n_sent_sms\": 0 }, { \"id\": 6, \"sms_send_ena\": 1, \"state\": \"registered\", \"imsi\": \"730030210913080\", \"n_sent_sms\": 0 }, { \"id\": 7, \"sms_send_ena\": 1, \"state\": \"registered\", \"imsi\": \"730030210913582\", \"n_sent_sms\": 0 }, { \"id\": 8, \"sms_send_ena\": 1, \"state\": \"registered\", \"imsi\": \"730030210888506\", \"n_sent_sms\": 0 } ], \"success\": true }";
            javax.json.JsonReader jsonReader = javax.json.Json.createReader(new java.io.StringReader(message));
            javax.json.JsonObject json = jsonReader.readObject();
            assertTrue(json.getBoolean("success"));
            javax.json.JsonArray arr = json.getJsonArray("channels");
            javax.json.JsonObject o = arr.getJsonObject(2);
            final String state = o.getString("state");
            assertNotNull(state);
            assertEquals("registered", state);
        }
    }

    @Ignore
    @Test
    public void testSendLongMessage() throws Exception {
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        String cmd = "api_queue_sms&destination=" + java.net.URLEncoder.encode("+56448909433", "UTF-8") + "&content=" + java.net.URLEncoder.encode("Lorem ipsum ad his scripta blandit partiendo, eum fastidii accumsan euripidis in, eum liber hendrerit an. Qui ut wisi vocibus suscipiantur, quo dicit ridens inciderint id. Quo mundi lobortis reformidans eu, legimus senserit definiebas an eos.", "UTF-8");
        String url = String.format("https://%s:%d/cgi-bin/sms_api?username=%s&password=%s&api_version=%s&cmd=%s", "smslyrics.americatotal.cl", 3084, "lyric_api", "lyric_api", "0.08", cmd);
        java.net.URL aURL = new java.net.URL(url);
        java.net.URLConnection aURLConnection = aURL.openConnection();
        aURLConnection.setDoOutput(true);

        try (java.io.InputStream is = aURLConnection.getInputStream(); javax.json.JsonReader jsonReader = javax.json.Json.createReader(is)) {
            javax.json.JsonObject json = jsonReader.readObject();
            assertFalse(json.getBoolean("success"));
            assertEquals("ContentTooLong", json.getString("error_code"));
        } catch (java.io.IOException e) {
            throw e;
        }
    }

    @Test
    @Ignore
    public void testSendShortMessage() throws Exception {
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        String cmd;
        int id = -1;
        final int port = 3084;
        /**/
        {
            cmd = "api_queue_sms&destination=" + java.net.URLEncoder.encode("+56448909433", "UTF-8") + "&content=" + java.net.URLEncoder.encode("Lorem ipsum ad his scripta blandit partiendo, eum fastidii accumsan euripidis in, eum liber hendrerit an.", "UTF-8");
            String url = String.format("https://%s:%d/cgi-bin/sms_api?username=%s&password=%s&api_version=%s&cmd=%s", "smslyrics.americatotal.cl", port, "lyric_api", "lyric_api", "0.08", cmd);
            java.net.URL aURL = new java.net.URL(url);
            java.net.URLConnection aURLConnection = aURL.openConnection();
            aURLConnection.setDoOutput(true);

            try (java.io.InputStream is = aURLConnection.getInputStream(); javax.json.JsonReader jsonReader = javax.json.Json.createReader(is)) {
                javax.json.JsonObject json = jsonReader.readObject();
                System.out.println(json);
                assertTrue(json.getBoolean("success"));
                id = json.getInt("message_id");
                assertTrue(id > 0);
                cmd = String.format("api_get_status&message_id=%d", id);
            } catch (java.io.IOException e) {
                throw e;
            }
        }//*/
        Thread.sleep(1000 * 15);
        {
            String url = String.format("https://%s:%d/cgi-bin/sms_api?username=%s&password=%s&api_version=%s&cmd=%s", "smslyrics.americatotal.cl", port, "lyric_api", "lyric_api", "0.08", cmd);
            java.net.URL aURL = new java.net.URL(url);
            java.net.URLConnection aURLConnection = aURL.openConnection();
            aURLConnection.setDoOutput(true);

            try (java.io.InputStream is = aURLConnection.getInputStream(); javax.json.JsonReader jsonReader = javax.json.Json.createReader(is)) {
                javax.json.JsonObject json = jsonReader.readObject();
                assertTrue(url, json.getBoolean("success"));
                assertEquals(2, json.getInt("message_status"));
            } catch (java.io.IOException e) {
                throw e;
            }
        }
        {
            cmd = String.format("api_sms_delete_by_id&sms_dir=out&id=%d", id);
            String url = String.format("https://%s:%d/cgi-bin/sms_api?username=%s&password=%s&api_version=%s&cmd=%s", "smslyrics.americatotal.cl", port, "lyric_api", "lyric_api", "0.08", cmd);
            java.net.URL aURL = new java.net.URL(url);
            java.net.URLConnection aURLConnection = aURL.openConnection();
            aURLConnection.setDoOutput(true);

            try (java.io.InputStream is = aURLConnection.getInputStream(); javax.json.JsonReader jsonReader = javax.json.Json.createReader(is)) {
                javax.json.JsonObject json = jsonReader.readObject();
                assertTrue(json.getBoolean("success"));
            } catch (java.io.IOException e) {
                throw e;
            }
        }

    }

    @Test
    @Ignore
    public void testSendDollarSign() throws Exception {
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        String cmd = "api_queue_sms&destination=" + java.net.URLEncoder.encode("+56986769674", "UTF-8") + "&content=" + java.net.URLEncoder.encode("$$$$", "UTF-8");
        String url = String.format("https://%s:%d/cgi-bin/sms_api?username=%s&password=%s&api_version=%s&cmd=%s", "smslyrics.americatotal.cl", 3084, "lyric_api", "lyric_api", "0.08", cmd);
        java.net.URL aURL = new java.net.URL(url);
        java.net.URLConnection aURLConnection = aURL.openConnection();
        aURLConnection.setDoOutput(true);

        try (java.io.InputStream is = aURLConnection.getInputStream(); javax.json.JsonReader jsonReader = javax.json.Json.createReader(is)) {
            javax.json.JsonObject json = jsonReader.readObject();
            assertFalse(json.getBoolean("success"));
            assertEquals("ContentTooLong", json.getString("error_code"));
        } catch (java.io.IOException e) {
            throw e;
        }
    }

    @Test
    @Ignore
    public void testSendAcuteAccent() throws Exception {
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        String cmd = "api_queue_sms&destination=" + java.net.URLEncoder.encode("+56986769674", "UTF-8") + "&content=" + java.net.URLEncoder.encode("Llegaría? <Lyric>", "UTF-8");
        String url = String.format("https://%s:%d/cgi-bin/sms_api?username=%s&password=%s&api_version=%s&cmd=%s", "smslyrics.americatotal.cl", 3084, "lyric_api", "lyric_api", "0.08", cmd);
        java.net.URL aURL = new java.net.URL(url);
        java.net.URLConnection aURLConnection = aURL.openConnection();
        aURLConnection.setDoOutput(true);

        try (java.io.InputStream is = aURLConnection.getInputStream(); javax.json.JsonReader jsonReader = javax.json.Json.createReader(is)) {
            javax.json.JsonObject json = jsonReader.readObject();
            assertFalse(json.getBoolean("success"));
            assertEquals("ContentTooLong", json.getString("error_code"));
        } catch (java.io.IOException e) {
            throw e;
        }
    }

}
