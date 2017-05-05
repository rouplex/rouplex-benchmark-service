package org.rouplex.service.benchmark;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class Util {
    // Simple matcher for durations as shown in https://en.wikipedia.org/wiki/ISO_8601#Durations
    private static final Pattern ISO_8601_SIMPLE =
            Pattern.compile("^P(\\d{1,2}Y)?(\\d{1,2}M)?(\\d{1,2}D)?(?:T(\\d{1,2}H)?(\\d{1,2}M)?(\\d{1,2}S)?)?$");
    private static final int[] FIELDS = new int[] {
            0, Calendar.YEAR, Calendar.MONTH, Calendar.DATE, Calendar.HOUR, Calendar.MINUTE, Calendar.SECOND
    };

    private static final SimpleDateFormat UTC_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public static Date getDateAfterDuration(String duration) throws ParseException {
        Matcher matcher = ISO_8601_SIMPLE.matcher(duration);
        if (!matcher.find()) {
            throw new ParseException("Cannot parse duration " + duration, 0);
        }

        Calendar calendar = Calendar.getInstance();
        for (int i = 1; i < matcher.groupCount(); i++) {
            String value = matcher.group(i);
            if (value != null) {
                calendar.add(FIELDS[i], Integer.parseInt(value.substring(0, value.length() - 1)));
            }
        }

        return calendar.getTime();
    }

    public static long convertIsoInstantToMillis(String isoInstant) throws ParseException {
        return UTC_DATE_FORMAT.parse(isoInstant).getTime();
    }

    public static String convertIsoInstantToString(long isoInstant) {
        return UTC_DATE_FORMAT.format(isoInstant);
    }
}