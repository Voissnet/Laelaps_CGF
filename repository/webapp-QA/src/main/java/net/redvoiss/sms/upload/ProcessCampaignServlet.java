package net.redvoiss.sms.upload;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Queue;
import static java.util.logging.Level.FINE;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.servlet.RequestDispatcher;

import org.apache.commons.text.StringEscapeUtils;

import net.redvoiss.sms.GenericHttpServlet;
import net.redvoiss.sms.upload.beans.EntryBean;
import net.redvoiss.sms.upload.beans.MessageBean;
import net.redvoiss.sms.upload.beans.AsyncReportBeanImpl;
import net.redvoiss.sms.upload.beans.CampaignBean;
import net.redvoiss.sms.upload.beans.ReportBean;
import net.redvoiss.sms.upload.beans.ReportBeanImpl;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static net.redvoiss.sms.upload.CampaignUploader.DESCRIPTION_NAME_MAX_LENGTH;
import static net.redvoiss.sms.upload.CampaignUploader.ENABLED;

import java.io.UnsupportedEncodingException;


/**
 * @see http://docs.oracle.com/javaee/6/tutorial/doc/glraq.html
 */
@WebServlet(urlPatterns = "/protected/campaign/process")
@MultipartConfig
public class ProcessCampaignServlet extends GenericHttpServlet implements net.redvoiss.sms.upload.CampaignUploader {

    private static final Logger LOGGER = Logger.getLogger(ProcessCampaignServlet.class.getName(),
            ProcessCampaignServlet.class.getName());
    private static final long serialVersionUID = 3L;
    private static final NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance();

    Map<String, Task> m_taskMap = new HashMap<>();

    @Resource
    ManagedExecutorService m_executor;

    @Resource(lookup = "jdbc/DB")
    private DataSource m_dataSource;

    public ProcessCampaignServlet() {
        super();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher requestDispatcher;
        List<CampaignBean> campaignBeanList = initializeAndResetAttribute(request, LOGGER);
        String hash = request.getParameter("campaign");
        if (hash == null) {
            ReportBean report = new ReportBeanImpl();
            report.addError("net.redvoiss.sms.upload.store.file_missing.error", hash);
            request.setAttribute("report", report);
            requestDispatcher = request.getRequestDispatcher("/protected/campaign/process.jsp");
        } else {
            CampaignBean selectedCampaignBean = new CampaignBean(hash);
            final int p = campaignBeanList.indexOf(selectedCampaignBean);
            if (p == -1) {
                LOGGER.warning(() -> String.format("No campaign found with hash {%s}", hash));
                ReportBean report = new ReportBeanImpl();
                report.addError("net.redvoiss.sms.upload.store.file_selected.error", hash);
                request.setAttribute("report", report);
                requestDispatcher = request.getRequestDispatcher("/protected/campaign/process.jsp");
            } else {
                requestDispatcher = request.getRequestDispatcher("/protected/campaign/result.jsp");
                CampaignBean campaignBean = campaignBeanList.remove(p);
                Task t = new Task(campaignBean, COMMERCIAL_VALUE.equals(request.getParameter("commercial")),
                        REPLY_VALUE.equals(request.getParameter("reply")), request.getParameter("date"), request.getParameter("encoding"),
                        request.getUserPrincipal().getName());
                t.setFuture(m_executor.submit(t));
                LOGGER.fine(() -> String.format("Storing task {%s} using hash {%s} as key", t.toString(), hash));
                m_taskMap.put(hash, t);
                request.setAttribute("taskId", hash);
            }
        }
        requestDispatcher.forward(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StringBuilder ret = new StringBuilder();
        try (PrintWriter out = response.getWriter()) {
            final String hash = request.getParameter("taskId");
            LOGGER.fine(() -> String.format("Searching for task {%s}", hash));
            if (m_taskMap.keySet().contains(hash)) {
                Task task = m_taskMap.get(hash);
                LOGGER.fine(() -> String.format("Found task {%s} for {%s}", task, hash));
                if (task == null) {
                    LOGGER.warning(String.format("Removing unnavailable task indexed by {%s}", hash));
                    m_taskMap.remove(hash);
                    ret.append("{\"action\": \"desist\"}");
                } else {
                    final String progress = String.format("\"progress\": \"%s\", ", task.getProgress());
                    if (task.getFuture().isDone()) {
                        final Queue<MessageBean> messages = task.getReport().getMessages();
                        if (messages.isEmpty()) {
                            LOGGER.fine(() -> String.format("Removing done task {%s}", task));
                            m_taskMap.remove(hash);
                            ret.append("{\"action\": \"concluding\", ");
                            ret.append(progress);
                            ret.append("\"elements\": []}");
                        } else {
                            ret.append("{\"action\": \"continue\", ");
                            ret.append(progress);
                            ret.append("\"elements\": [");
                            final int size = messages.size();
                            LOGGER.log(INFO, "Current size is {0}", size);
                            for (int i = 0; i < size && i < 10; i++) {
                                MessageBean messageBean = messages.remove();
                                if (messageBean == null) {
                                    LOGGER.log(WARNING, "Found empty message for task {0}", hash);
                                } else {
                                    if (i == 0) {
                                        //do nothing
                                    } else {
                                        ret.append(", ");
                                    }
                                    ret.append(String.format("{\"text\": \"%s\", \"type\": \"%s\"}",
                                            messageBean.getMessage(), messageBean.getType().toString()));
                                }
                            }
                            ret.append("]}");
                        }
                    } else {
                        LOGGER.fine(() -> String.format("Task {%s} is still running", task));
                        ret.append("{\"action\": \"continue\", ");
                        ret.append(progress);
                        MessageBean messageBean = task.getReport().getMessages().poll();
                        if (messageBean == null) {
                            ret.append("\"elements\": []}");
                        } else {
                            ret.append(String.format("\"elements\": [{\"text\": \"%s\", \"type\": \"%s\" }]}",
                                    messageBean.getMessage(), messageBean.getType().toString()));
                        }
                    }
                }
            } else {
                LOGGER.warning(String.format("No task available associated with {%s}", hash));
                ret.append("{\"action\": \"desist\"}");
            }
            LOGGER.finest(() -> String.format("Process feedback is : {%s}", ret));
            out.print(ret.toString());
        }
    }

    class Task implements Callable<Long> {

        private int count, total;
        CampaignBean campaignBean;
        String fileEncoding, username;
        Date scheduledDate;
        boolean isCommercial, isReplyEnabled;
        Future<Long> future;
        ReportBean report = new AsyncReportBeanImpl();

        Task(CampaignBean campaignBean, boolean isCommercial, boolean isReplyEnabled, String date, String fileEncoding, String username) {
            this.campaignBean = campaignBean;
            this.isCommercial = isCommercial;
            this.isReplyEnabled = isReplyEnabled;
            this.fileEncoding = fileEncoding;
            this.username = username;
            try {
                this.scheduledDate = date == null ? new Date() : DATE_FORMATTER.parse(date);
            } catch (ParseException pe) {
                LOGGER.log(SEVERE, String.format("Unexpected exception while parsing date {%s}", date), pe);
                report.addError("net.redvoiss.sms.upload.store.date_format.error", date);
            }
        }

        void setFuture(Future<Long> future) {
            this.future = future;
        }

        Future<Long> getFuture() {
            return future;
        }

        ReportBean getReport() {
            return report;
        }

        String getProgress() {
            return "<dd>" + PERCENT_FORMAT.format(count / (double) total) + "</dd>";
        }

        @Override
        public Long call() {
            final long startTime = System.nanoTime();
            List<EntryBean> entryList = getFileEntryList(campaignBean.getFile(), fileEncoding);
            store(entryList, username, campaignBean.getName(), scheduledDate, isCommercial, isReplyEnabled);
            return System.nanoTime() - startTime;
        }

        /**
         * @see
         * https://stackoverflow.com/questions/696626/java-filereader-encoding-issue
         */
        private synchronized List<EntryBean> getFileEntryList(File file, String fileEncoding) {
            List<EntryBean> ret = new ArrayList<>();
            LOGGER.fine(
                    () -> String.format("About to read file {%s} using {%s} encoding", file.getName(), fileEncoding));
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), fileEncoding))) {
                String str;
                while ((str = br.readLine()) != null) {
                    Matcher contentMatcher = CONTENT_PATTERN.matcher(str);
                    if (contentMatcher.matches() && contentMatcher.groupCount() == 2) {
                        final String destination = contentMatcher.group(1);
                        final String message = contentMatcher.group(2);
                        ret.add(new EntryBean(destination, message));
                        LOGGER.log(FINE, "net.redvoiss.sms.upload.entry.ok",
                                new Object[]{file.getName(), destination, message});
                    } else {
                        LOGGER.log(WARNING, "net.redvoiss.sms.upload.entry.nok",
                                new Object[]{CONTENT_PATTERN.pattern(), str});
                        report.addWarning("net.redvoiss.sms.store.invalid_entry", StringEscapeUtils.escapeJson(str));
                    }
                }
            } catch (IOException ioe) {
                LOGGER.log(SEVERE,
                        String.format("Unexpected IO exception while trying to read file {%s} using encoding {%s}",
                                file.getName(), fileEncoding),
                        ioe);
            }
            return ret;
        }

        @javax.transaction.Transactional(rollbackOn = {SQLException.class})
        protected synchronized void store(List<EntryBean> fileEntries, final String username, final String filename,
                Date date, boolean isCommercial, boolean isReplyEnabled) {
            final long startTime = System.nanoTime();
            total = fileEntries.size();
            if (total > 0) {
                LOGGER.info(String.format("About to store file {%s} for user {%s} containing {%s} entries", filename,
                        username, total));
                String batchId = null;
                String destination = "";
                try (Connection conn = m_dataSource.getConnection()) {
                    for (count = 0; count < total; count++) 
                    {
                        EntryBean eb = fileEntries.get(count);
                        if (ENABLED) {
                            LOGGER.log(FINE, "Reply enabled: {0}", isReplyEnabled);
                            destination = eb.getDestination();
                            try
                            {
                                if (isReplyEnabled) {
                                    batchId = net.redvoiss.sms.dao.DAO.creaDespachaLoteCompletoReply(conn, username, eb.getDestination(),
                                                new String(eb.getMessage().getBytes("UTF-8"), "ISO-8859-1") , "", batchId, count + 1 == total,
                                                filename.length() > DESCRIPTION_NAME_MAX_LENGTH  ? 
                                                        filename.substring(0, DESCRIPTION_NAME_MAX_LENGTH) : filename, 
                                                "", isCommercial, date,"");
                                } else {
                                    batchId = net.redvoiss.sms.dao.DAO.creaDespachaLoteCompleto(conn, username, eb.getDestination(),
                                                new String(eb.getMessage().getBytes("UTF-8"), "ISO-8859-1"), "", batchId, count + 1 == total,
                                                filename.length() > DESCRIPTION_NAME_MAX_LENGTH  ?
                                                        filename.substring(0, DESCRIPTION_NAME_MAX_LENGTH) : filename,
                                                "", isCommercial, date,"");
                                }
                            } catch (UnsupportedEncodingException uee) {
                                    LOGGER.log(SEVERE, String.format("UnsupportedEncodingException  exception while processing {%s}", filename), uee);
                                    final long elapsedTime = System.nanoTime() - startTime;                    
                                    LOGGER.log(WARNING, String.format("UnsupportedEncodingException text to store message to %s to destintaion %s", username, destination), uee);

                            } catch (SQLException sqle) {
                                    LOGGER.log(SEVERE, String.format("Unexpected DB exception while processing {%s}", filename), sqle);
                                    final long elapsedTime = System.nanoTime() - startTime;                    
                                    if (sqle.getErrorCode() == 20001) {//Balance too low
                                        LOGGER.log(WARNING, String.format("Balance too low to store message to %s", username), sqle);
                                        report.addMessage("net.redvoiss.sms.upload.store.destination.insufficient_balance", destination);
                                    }
                                    else if (sqle.getErrorCode() == 20002) {//Mobile number is on Blacklist
                                        LOGGER.log(WARNING, String.format("Mobile %s is on blacklist", destination), sqle);
                                        report.addMessage("net.redvoiss.sms.upload.store.destination.at_blacklist", destination);
                                    }
                            }
                        } else {
                            LOGGER.warning("DB access is disabled");
                        }
                    }
                    final long elapsedTime = System.nanoTime() - startTime;
                    report.addMessage("net.redvoiss.sms.upload.store.elapsed_time", batchId, getElapsedTime(elapsedTime));
                    LOGGER.info(String.format("Stored file {%s} with {%d} records for user {%s}. Batch was {%s}", filename,
                            count, username, batchId));
                    //m_mailReporter.reportCampaignUpload( batchId, String.valueOf(report) );
                } catch (SQLException sqle) {
                    LOGGER.log(SEVERE, String.format("Unexpected DB exception while processing {%s}", filename), sqle);
                    final long elapsedTime = System.nanoTime() - startTime;
                    if (batchId == null) {
                        report.addError("net.redvoiss.sms.upload.store.db_error", getElapsedTime(elapsedTime),
                                sqle.getMessage());
                    } else {
                        report.addError("net.redvoiss.sms.upload.store.batch.error", batchId, getElapsedTime(elapsedTime),
                                sqle.getMessage());
                    }                                       
                } finally {
                    report.addMessage("net.redvoiss.sms.upload.store.number_of_records", count);
                }
            } else {
                report.addWarning("net.redvoiss.sms.upload.store.missing_campaign.error", filename);
            }
        }
    }
}
