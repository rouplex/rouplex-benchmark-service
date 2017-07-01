package org.rouplex.service.benchmark;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
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
    private static final SimpleDateFormat LOCAL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US);

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

    public static String convertMillisToIsoInstant(long millis, Integer timeOffsetInMinutes) {
        if (timeOffsetInMinutes == null) {
            return UTC_DATE_FORMAT.format(millis);
        }

        TimeZone timeZone = TimeZone.getTimeZone("GMT");
        timeZone.setRawOffset(timeOffsetInMinutes * 60 * 1000);

        GregorianCalendar calendar = new GregorianCalendar(timeZone);
        calendar.setTimeInMillis(millis);
        return LOCAL_DATE_FORMAT.format(calendar.getTime());
    }

    public static void checkNonNullArg(Object val, String fieldName) {
        if (val == null) {
            throw new IllegalArgumentException(String.format("Argument %s cannot be null", fieldName));
        }
    }

    public static void checkNonNegativeArg(int val, String fieldName) {
        if (val < 0) {
            throw new IllegalArgumentException(String.format("Argument %s cannot be negative", fieldName));
        }
    }

    public static void checkPositiveArg(int val, String fieldName) {
        if (val <= 0) {
            throw new IllegalArgumentException(String.format(
                    "Argument %s cannot be less than 1", fieldName));
        }
    }

    public static void checkPositiveArgDiff(int val, String fieldName1, String fieldName2) {
        if (val <= 0) {
            throw new IllegalArgumentException(String.format(
                    "Argument %s cannot be greater or equal than argument %s", fieldName1, fieldName2));
        }
    }
}
