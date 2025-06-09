package net.redvoiss.sms.upload;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import java.security.MessageDigest;
import java.security.DigestInputStream;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.RequestDispatcher;

import net.redvoiss.sms.GenericHttpServlet;
import net.redvoiss.sms.upload.beans.CampaignBean;
import net.redvoiss.sms.upload.beans.ReportBean;
import net.redvoiss.sms.upload.beans.ReportBeanImpl;

import static java.util.logging.Level.SEVERE;

/**
 * @see http://docs.oracle.com/javaee/6/tutorial/doc/glraq.html
 */
@WebServlet(urlPatterns = "/protected/campaign/upload")
@MultipartConfig
public class UploadCampaignServlet extends GenericHttpServlet implements net.redvoiss.sms.upload.CampaignUploader {

    private static final Logger LOGGER = Logger.getLogger(UploadCampaignServlet.class.getName());
    private static final long serialVersionUID = 2L;

    public UploadCampaignServlet() {
        super();
    }

    /**
     * @param request
     * @param response
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     * @see http://www.javacodex.com/Files/Temporary-Files
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher requestDispatcher = request.getRequestDispatcher("/protected/campaign/process.jsp");
        LOGGER.fine(() -> "About to upload files");
        List<CampaignBean> campaignBeanList = initializeAndResetAttribute(request, LOGGER);
        ReportBean uploadReport = initializeReportingAttribute(request);
        request.setAttribute("report", uploadReport);
        for (Part part : request.getParts()) {
            final String filename = getFileName(part);
            if (filename == null) {
                uploadReport.addError("net.redvoiss.sms.upload.filename.invalid_name");
            } else {
                File tmpFile = File.createTempFile("campaign", ".tmp");
                tmpFile.deleteOnExit();
                try {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    try (InputStream is = request.getPart(part.getName()).getInputStream()) {
                        try (FileOutputStream out = new FileOutputStream(tmpFile);
                                DigestInputStream dis = new DigestInputStream(is, md)) {
                            int read, length = 0;
                            final byte[] bytes = new byte[1024];
                            while ((read = dis.read(bytes)) != -1) {
                                out.write(bytes, 0, read);
                                length += read;
                            }
                            if (length > 0) {
                                final byte[] hashb = md.digest();
                                final String hashs = hashCodeAsString(hashb);
                                LOGGER.fine(String.format("Succesfully uploaded file {%s} with size {%s} and MD5 {%s}",
                                        filename, length, hashs));
                                campaignBeanList.add(new CampaignBean(filename, tmpFile, length, hashs));
                            } else {
                                uploadReport.addError("net.redvoiss.sms.upload.filename.empty");
                            }
                        }
                    }
                } catch (NoSuchAlgorithmException e) {
                    LOGGER.log(SEVERE, String.format("Unexpected exception while uploading file {%s}", filename), e);
                    uploadReport.addError("net.redvoiss.sms.upload.file.error", filename);
                    if (tmpFile.delete()) {
                        LOGGER.fine(() -> "Deleted unused temporary file");
                    } else {
                        LOGGER.warning(() -> "Unable to delete temporary file");
                    }
                }
            }
        }
        requestDispatcher.forward(request, response);
    }

    private static ReportBean initializeReportingAttribute(HttpServletRequest request) {
        ReportBean ret = new ReportBeanImpl();
        return ret;
    }

    private static String getFileName(Part part) {
        String partHeader = part.getHeader("content-disposition");
        LOGGER.log(Level.FINE, "Part Header = {0}", partHeader);
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    protected static String hashCodeAsString(byte[] hashCode) {
        final String ret = new java.math.BigInteger(1, hashCode).toString(16);
        return ret;
    }
}
