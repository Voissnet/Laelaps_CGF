package net.redvoiss.sms.smpp.dlr;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jorge Avila
 */
public abstract class AbstractDlrParserImpl implements DlrParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDlrParserImpl.class);
    private static final Locale LOCALE = Locale.US;

    Optional<String> id = Optional.empty(), sub = Optional.empty(), dlvrd = Optional.empty(), err = Optional.empty(), text = Optional.empty();
    Optional<Date> submitDate = Optional.empty(), doneDate = Optional.empty();
    Optional<StatEnum> stat = Optional.empty();

    private final String dlr;

    /**
     * Establishes date format
     * @return
     */
    protected abstract SimpleDateFormat getSimpleDateFormat();

    /**
     * Specifies general behavior
     *
     * @param dlr
     */
    public AbstractDlrParserImpl(String dlr) {
        this.dlr = dlr;
    }

    /**
     * In case we need to override it
     *
     * @return
     */
    protected String getDlr() {
        return dlr == null ? null : dlr.replace('\n', '\0').replace('\r', '\0').toLowerCase(LOCALE);
    }

    /**
     * Parses Id field
     *
     * @return id field value
     */
    @Override
    public Optional<String> parseIdField() {
        final String noEndlineDlr = getDlr();
        if (noEndlineDlr == null) {
            LOGGER.warn("Unable to process null input");
        } else if (id.isPresent()) {
            LOGGER.debug("Id is already stored");
        } else {
            final String pattern = ".*id:(\\S+).*";
            Matcher m = Pattern.compile(pattern).matcher(noEndlineDlr);
            if (m.matches()) {
                id = Optional.of(m.group(1));
            }
        }
        return id;
    }

    @Override
    public Optional<String> parseSubField() {
        return sub;
    }

    @Override
    public Optional<String> parseDlvrdField() {
        return dlvrd;
    }

    @Override
    public Optional<Date> parseSubmitDateField() throws ParseException {
        final String noEndlineDlr = getDlr();
        if (noEndlineDlr == null) {
            LOGGER.warn("Unable to process null input");
        } else if (submitDate.isPresent()) {
            LOGGER.debug("Submit date already stored");
        } else {
            final String pattern = ".*submit date:(\\S+).*";
            Matcher m = Pattern.compile(pattern).matcher(noEndlineDlr);
            if (m.matches()) {
                submitDate = Optional.of(getSimpleDateFormat().parse(m.group(1)));
            }
        }
        return submitDate;
    }

    @Override
    public Optional<Date> parseDoneDateField() throws ParseException {
        final String noEndlineDlr = getDlr();
        if (noEndlineDlr == null) {
            LOGGER.warn("Unable to process null input");
        } else if (doneDate.isPresent()) {
            LOGGER.debug("Done date already stored");
        } else {
            final String pattern = ".*done date:(\\S+).*";
            Matcher m = Pattern.compile(pattern).matcher(noEndlineDlr);
            if (m.matches()) {
                doneDate = Optional.of(getSimpleDateFormat().parse(m.group(1)));
            }
        }
        return doneDate;
    }

    @Override
    public Optional<StatEnum> parseStatField() {
        final String noEndlineDlr = getDlr();
        if (noEndlineDlr == null) {
            LOGGER.warn("Unable to process null input");
        } else if (stat.isPresent()) {
            LOGGER.debug("Stat is already stored");
        } else {
            final String pattern = ".*stat:(\\S+).*";
            Matcher m = Pattern.compile(pattern).matcher(noEndlineDlr);
            if (m.matches()) {
                stat = Optional.of(StatEnum.valueOf(m.group(1).toUpperCase(LOCALE)));
            }
        }
        return stat;
    }

    @Override
    public Optional<String> parseErrField() {
        final String noEndlineDlr = getDlr();
        if (noEndlineDlr == null) {
            LOGGER.warn("Unable to process null input");
        } else if (err.isPresent()) {
            LOGGER.debug("Err is already stored");
        } else {
            final String pattern = ".*err:(\\S+).*";
            Matcher m = Pattern.compile(pattern).matcher(noEndlineDlr);
            if (m.matches()) {
                err = Optional.of(m.group(1));
            }
        }
        return err;
    }

    @Override
    public Optional<String> parseTextField() {
        final String noEndlineDlr = getDlr();
        if (noEndlineDlr == null) {
            LOGGER.warn("Unable to process null input");
        } else if (text.isPresent()) {
            LOGGER.debug("Err is already stored");
        } else {
            final String textPattern = "text:";
            final int p = noEndlineDlr.indexOf(textPattern);
            text = Optional.of(dlr.substring(p + textPattern.length()));
        }
        return text;
    }

    @Override
    public String toString() {
        return dlr;
    }

}
