package net.redvoiss.sms.upload;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.time.DurationFormatUtils;

import net.redvoiss.sms.upload.beans.CampaignBean;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public interface CampaignUploader {

    static Pattern CONTENT_PATTERN = Pattern.compile("^(\\d{11})(?:,|;)(.{1,160})$");
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    static DateFormat DATE_FORMATTER = new SimpleDateFormat(DATE_FORMAT);
    static final boolean ENABLED = !Boolean.parseBoolean(System.getenv("DEVELOPMENT_MODE"));
    static int DESCRIPTION_NAME_MAX_LENGTH = 50;
    static final String ATTR_NAME = "uploadedCampaigns";
    static final String COMMERCIAL_VALUE = "XYZ";
    static final String REPLY_VALUE = "GHJ";

    default String getElapsedTime(long elapsedTime) {
        return DurationFormatUtils.formatDuration(MILLISECONDS.convert(elapsedTime, NANOSECONDS), "HH:mm:ss.S");
    }

    /**
     * @param request
     * @param logger
     * @return
     * @see https://stackoverflow.com/a/28953757
     */
    default List<CampaignBean> initializeAndResetAttribute(HttpServletRequest request, Logger logger) {
        List<CampaignBean> ret = new ArrayList<>();
        Object o = request.getSession().getAttribute(ATTR_NAME);
        if (o == null) {
            logger.fine(() -> "Attribute is missing");
        } else {
            ((List<?>) o).forEach((x) -> {
                ret.add((CampaignBean) x);
            });
        }
        request.getSession().setAttribute(ATTR_NAME, ret);
        return ret;
    }
}
