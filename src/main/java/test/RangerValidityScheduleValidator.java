package test;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class RangerValidityScheduleValidator {

    private static final Log LOG = LogFactory.getLog(RangerValidityScheduleValidator.class);

    private static final ThreadLocal<DateFormat> DATE_FORMATTER = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat(RangerValiditySchedule.VALIDITY_SCHEDULE_DATE_STRING_SPECIFICATION);
        }
    };

    final private RangerValiditySchedule validitySchedule;
    private Date startTime;
    private Date endTime;

    private RangerValidityRecurrence.RecurrenceSchedule validityPeriodEstimator;
    private RangerValiditySchedule normalizedValiditySchedule;

    private static List<String> validTimeZoneIds;

    static {
        validTimeZoneIds = Arrays.asList(TimeZone.getAvailableIDs());
    }

    public RangerValidityScheduleValidator(RangerValiditySchedule validitySchedule) {
        this.validitySchedule = validitySchedule;
        if (validitySchedule != null && validitySchedule.getStartTime() != null && validitySchedule.getEndTime() != null) {
            try {
                startTime = DATE_FORMATTER.get().parse(validitySchedule.getStartTime());
                endTime = DATE_FORMATTER.get().parse(validitySchedule.getEndTime());
            } catch (ParseException exception) {
                LOG.error("Error parsing startTime:[" + validitySchedule.getStartTime() + "], and/or "
                        + "endTime:[" + validitySchedule.getEndTime() + "]", exception);
            }
        }
    }

    public RangerValiditySchedule validate(List<ValidationFailureDetails> validationFailures) {
        RangerValiditySchedule ret = null;

        if (validitySchedule != null) {
            if (startTime == null || endTime == null) {
                validationFailures.add(new ValidationFailureDetails(0, "startTime", "", true, true, false, "empty/null values"));
            } else {

                validityPeriodEstimator = new RangerValidityRecurrence.RecurrenceSchedule();
                normalizedValiditySchedule = new RangerValiditySchedule();

                boolean isValid = validateTimeRangeSpec(validationFailures);
                if (isValid) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("validityPeriodEstimator:[" + validityPeriodEstimator + "]");
                    }
                    normalizedValiditySchedule.setStartTime(validitySchedule.getStartTime());
                    normalizedValiditySchedule.setEndTime(validitySchedule.getEndTime());
                    normalizedValiditySchedule.setTimeZone(validitySchedule.getTimeZone());

                    ret = normalizedValiditySchedule;
                }
            }
        } else {
            validationFailures.add(new ValidationFailureDetails(0, "", "", true, true, false, "validitySchedule is empty/null"));
        }
        return ret;
    }

    private boolean validateTimeRangeSpec(List<ValidationFailureDetails> validationFailures) {
        boolean ret;
        if (startTime.getTime() >= endTime.getTime()) {
            validationFailures.add(new ValidationFailureDetails(0, "startTime", "", false, true, false, "startTime later than endTime"));
            ret = false;
        } else {
            ret = true;
        }
        ret = validateTimeZone(validitySchedule.getTimeZone(), validationFailures) && ret;

        for (RangerValidityRecurrence recurrence : validitySchedule.getRecurrences()) {
            ret = validateValidityInterval(recurrence, validationFailures) && ret;

            if (RangerValidityRecurrence.ValidityInterval.getValidityIntervalInMinutes(recurrence.getInterval()) > 0) {
                ret = validateFieldSpec(recurrence, RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.minute, validationFailures) && ret;
                ret = validateFieldSpec(recurrence, RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.hour, validationFailures) && ret;
                ret = validateFieldSpec(recurrence, RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfMonth, validationFailures) && ret;
                ret = validateFieldSpec(recurrence, RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfWeek, validationFailures) && ret;
                ret = validateFieldSpec(recurrence, RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.month, validationFailures) && ret;
                ret = validateFieldSpec(recurrence, RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.year, validationFailures) && ret;
                ret = ret && validateIntervalDuration(recurrence, validationFailures);

                if (ret) {
                    RangerValidityRecurrence.RecurrenceSchedule schedule = new RangerValidityRecurrence.RecurrenceSchedule(getNormalizedValue(recurrence, RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.minute), getNormalizedValue(recurrence, RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.hour),
                            getNormalizedValue(recurrence, RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfMonth), getNormalizedValue(recurrence, RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfWeek),
                            getNormalizedValue(recurrence, RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.month), getNormalizedValue(recurrence, RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.year));
                    RangerValidityRecurrence normalizedRecurrence = new RangerValidityRecurrence(schedule, recurrence.getInterval());
                    normalizedValiditySchedule.getRecurrences().add(normalizedRecurrence);

                }
            }
        }

        return ret;
    }

    private boolean validateTimeZone(String timeZone, List<ValidationFailureDetails> validationFailures) {
        boolean ret = false;
        if (StringUtils.isNotBlank(timeZone)) {
            for (String validZone : validTimeZoneIds) {
                if (StringUtils.equals(timeZone, validZone)) {
                    ret = true;
                    break;
                }
            }
        } else {
            ret = true;
        }
        if (!ret) {

        }
        return ret;
    }

    private boolean validateValidityInterval(RangerValidityRecurrence recurrence, List<ValidationFailureDetails> validationFailures) {
        boolean ret = true;
        RangerValidityRecurrence.ValidityInterval validityInterval = recurrence.getInterval();
        if (validityInterval != null) {
            if (validityInterval.getDays() < 0
                    || (validityInterval.getHours() < 0 || validityInterval.getHours() > 23)
                    || (validityInterval.getMinutes() < 0 || validityInterval.getMinutes() > 59)) {
                validationFailures.add(new ValidationFailureDetails(0, "interval", "", false, true, false, "invalid interval"));
                ret = false;
            }
        }
        int validityIntervalInMinutes = RangerValidityRecurrence.ValidityInterval.getValidityIntervalInMinutes(validityInterval);
        if (validityIntervalInMinutes > 0) {
            if (StringUtils.isBlank(recurrence.getSchedule().getDayOfMonth()) && StringUtils.isBlank(recurrence.getSchedule().getDayOfWeek())) {
                validationFailures.add(new ValidationFailureDetails(0, "validitySchedule", "", false, true, false, "empty dayOfMonth and dayOfWeek"));
                ret = false;
            }
        }
        return ret;
    }

    private boolean validateFieldSpec(RangerValidityRecurrence recurrence, RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec field, List<ValidationFailureDetails> validationFailures) {
        boolean ret = true;

        String fieldValue = recurrence.getSchedule().getFieldValue(field);
        if (StringUtils.isBlank(fieldValue)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No value provided for [" + field + "]");
            }
            if (StringUtils.equals(field.name(), RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfWeek.name())
                    || StringUtils.equals(field.name(), RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfMonth.name())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Allow blank value for dayOfWeek or dayOfMonth here. Check for both being null is done elsewhere.");
                }
            } else {
                validationFailures.add(new ValidationFailureDetails(0, field.toString(), "", false, true, false, "No value provided"));
            }
        }
        ret = validateCharacters(fieldValue, field.specialChars);

        if (!ret) {
            validationFailures.add(new ValidationFailureDetails(0, field.toString(), "", false, true, false, "invalid character(s)"));
        } else {
            // Valid month values in java.util.Date are from 1-12, in java.util.Calendar from 0 to 11
            // Internally we use Calendar values for validation and evaluation
            int minimum = field == RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.month ? field.minimum + 1 : field.minimum;
            int maximum = field == RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.month ? field.maximum + 1 : field.maximum;
            ret = validateRanges(recurrence, field, minimum, maximum, validationFailures);
        }
        return ret;
    }

    private boolean validateCharacters(String str, String permittedCharacters) {
        boolean ret = true;
        char[] chars = str.toCharArray();
        for (char c : chars) {
            if (!(Character.isDigit(c) || Character.isWhitespace(c) || StringUtils.contains(permittedCharacters, c))) {
                ret = false;
                break;
            }
        }
        return ret;
    }

    private boolean validateIntervalDuration(RangerValidityRecurrence recurrence, List<ValidationFailureDetails> validationFailures) {
        boolean ret = true;

        if (!validationFailures.isEmpty() || validityPeriodEstimator == null) {
            ret = false;
        } else {
            int minSchedulingInterval = 1; // In minutes

            String minutes = validityPeriodEstimator.getFieldValue(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.minute);
            if (!StringUtils.equals(minutes, RangerValidityRecurrence.RecurrenceSchedule.WILDCARD)) {
                minSchedulingInterval = StringUtils.isBlank(minutes) ? RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.minute.maximum + 1 : Integer.valueOf(minutes);

                if (minSchedulingInterval == RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.minute.maximum + 1) {
                    String hours = validityPeriodEstimator.getFieldValue(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.hour);
                    if (!StringUtils.equals(hours, RangerValidityRecurrence.RecurrenceSchedule.WILDCARD)) {
                        int hour = StringUtils.isBlank(hours) ? RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.hour.maximum + 1 :Integer.valueOf(hours);
                        minSchedulingInterval = hour * (RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.minute.maximum+1);

                        if (hour == RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.hour.maximum + 1) {
                            String dayOfMonths = validityPeriodEstimator.getFieldValue(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfMonth);
                            String dayOfWeeks = validityPeriodEstimator.getFieldValue(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfWeek);

                            int dayOfMonth = 1, dayOfWeek = 1;
                            if (!StringUtils.equals(dayOfMonths, RangerValidityRecurrence.RecurrenceSchedule.WILDCARD)) {
                                dayOfMonth = StringUtils.isBlank(dayOfMonths) ? RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfMonth.maximum + 1 : Integer.valueOf(dayOfMonths);
                            }
                            if (!StringUtils.equals(dayOfWeeks, RangerValidityRecurrence.RecurrenceSchedule.WILDCARD)) {
                                dayOfWeek = StringUtils.isBlank(dayOfWeeks) ? RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfWeek.maximum + 1 : Integer.valueOf(dayOfWeeks);
                            }
                            if (!StringUtils.equals(dayOfMonths, RangerValidityRecurrence.RecurrenceSchedule.WILDCARD) || !StringUtils.equals(dayOfWeeks, RangerValidityRecurrence.RecurrenceSchedule.WILDCARD)) {
                                int minDays = dayOfMonth > dayOfWeek ? dayOfWeek : dayOfMonth;
                                minSchedulingInterval = minDays*(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.hour.maximum+1)*(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.minute.maximum+1);

                                if (dayOfMonth == (RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfMonth.maximum+1) && dayOfWeek == (RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfWeek.maximum+1)) {
                                    String months = validityPeriodEstimator.getFieldValue(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.month);
                                    if (!StringUtils.equals(months, RangerValidityRecurrence.RecurrenceSchedule.WILDCARD)) {
                                        int month = StringUtils.isBlank(months) ? RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.month.maximum + 1 :Integer.valueOf(months);
                                        minSchedulingInterval = month * 28 * (RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.hour.maximum + 1) * (RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.minute.maximum + 1);

                                        if (month == RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.month.maximum + 1) {
                                            // Maximum interval is 1 year
                                            minSchedulingInterval = 365 * (RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.hour.maximum + 1) * (RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.minute.maximum + 1);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (RangerValidityRecurrence.ValidityInterval.getValidityIntervalInMinutes(recurrence.getInterval()) > minSchedulingInterval) {
                if (LOG.isDebugEnabled()) {
                    LOG.warn("Specified scheduling interval:" + RangerValidityRecurrence.ValidityInterval.getValidityIntervalInMinutes(recurrence.getInterval()) + " minutes] is more than minimum possible scheduling interval:[" + minSchedulingInterval + " minutes].");
                    LOG.warn("This may turn this (expected to be temporary) policy into effectively permanent policy.");
                }
            }
        }
        return ret;
    }

    private boolean validateRanges(RangerValidityRecurrence recurrence, RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec field, int minValidValue, int maxValidValue, List<ValidationFailureDetails> validationFailures) {
        boolean ret = true;

        String value = null;
        String fieldName = field.toString();

        String noWhiteSpace = StringUtils.deleteWhitespace(recurrence.getSchedule().getFieldValue(field));
        String[] specs = StringUtils.split(noWhiteSpace, ",");
        class Range {
            private int lower;
            private int upper;
            private Range(int lower, int upper) {
                this.lower = lower;
                this.upper = upper;
            }
        }
        class RangeComparator implements Comparator<Range>, Serializable {
            @Override
            public int compare(Range me, Range other) {
                int result;
                result = Integer.compare(me.lower, other.lower);
                if (result == 0) {
                    result = Integer.compare(me.upper, other.upper);
                }
                return result;
            }
        }

        List<Range> rangeOfValues = new ArrayList<>();

        List<Integer> values = new ArrayList<>();

        for (String spec : specs) {

            if (StringUtils.isNotEmpty(spec)) {
                // Range
                if (spec.startsWith("-") || spec.endsWith("-")) {
                    validationFailures.add(new ValidationFailureDetails(0, fieldName, "", false, true, false, "incorrect range spec"));
                    ret = false;
                } else {
                    String[] ranges = StringUtils.split(spec, "-");
                    if (ranges.length > 2) {
                        validationFailures.add(new ValidationFailureDetails(0, fieldName, "", false, true, false, "incorrect range spec"));
                        ret = false;
                    } else if (ranges.length == 2) {
                        int val1 = minValidValue, val2 = maxValidValue;
                        if (!StringUtils.equals(ranges[0], RangerValidityRecurrence.RecurrenceSchedule.WILDCARD)) {
                            val1 = Integer.valueOf(ranges[0]);
                            if (val1 < minValidValue || val1 > maxValidValue) {
                                validationFailures.add(new ValidationFailureDetails(0, fieldName, "", false, true, false, "incorrect lower range value"));
                                ret = false;
                            }
                        } else {
                            value = RangerValidityRecurrence.RecurrenceSchedule.WILDCARD;
                        }
                        if (!StringUtils.equals(ranges[1], RangerValidityRecurrence.RecurrenceSchedule.WILDCARD)) {
                            val2 = Integer.valueOf(ranges[1]);
                            if (val1 < minValidValue || val2 > maxValidValue) {
                                validationFailures.add(new ValidationFailureDetails(0, fieldName, "", false, true, false, "incorrect upper range value"));
                                ret = false;
                            }
                        } else {
                            value = RangerValidityRecurrence.RecurrenceSchedule.WILDCARD;
                        }
                        if (ret) {
                            if (val1 >= val2) {
                                validationFailures.add(new ValidationFailureDetails(0, fieldName, "", false, true, false, "incorrect range"));
                                ret = false;
                            } else {
                                value = RangerValidityRecurrence.RecurrenceSchedule.WILDCARD;
                                for (Range range : rangeOfValues) {
                                    if (range.lower == val1 || range.upper == val2) {
                                        validationFailures.add(new ValidationFailureDetails(0, fieldName, "", false, true, false, "duplicate range"));
                                        ret = false;
                                        break;
                                    }
                                }
                                if (ret) {
                                    rangeOfValues.add(new Range(val1, val2));
                                }
                            }
                        }
                    } else if (ranges.length == 1) {
                        if (!StringUtils.equals(ranges[0], RangerValidityRecurrence.RecurrenceSchedule.WILDCARD)) {
                            int val = Integer.valueOf(ranges[0]);
                            if (val < minValidValue || val > maxValidValue) {
                                validationFailures.add(new ValidationFailureDetails(0, fieldName, "", false, true, false, "incorrect value"));
                                ret = false;
                            } else {
                                if (!StringUtils.equals(value, RangerValidityRecurrence.RecurrenceSchedule.WILDCARD)) {
                                    values.add(Integer.valueOf(ranges[0]));
                                }
                            }
                        } else {
                            value = RangerValidityRecurrence.RecurrenceSchedule.WILDCARD;
                        }
                    } else {
                        ret = false;
                    }
                }
            }
        }
        //if (ret) {
            if (CollectionUtils.isNotEmpty(rangeOfValues)) {
                rangeOfValues.sort( new RangeComparator());
            }
            for (int i = 0; i < rangeOfValues.size(); i++) {
                Range range = rangeOfValues.get(i);
                int upper = range.upper;
                for (int j = i+1; j < rangeOfValues.size(); j++) {
                    Range r = rangeOfValues.get(j);
                    if (upper < r.upper) {
                        validationFailures.add(new ValidationFailureDetails(0, fieldName, "", false, true, false, "overlapping range value"));
                        ret = false;
                    }
                }
            }
        //}
        if (ret) {
            if (!StringUtils.equals(value, RangerValidityRecurrence.RecurrenceSchedule.WILDCARD)) {

                int minDiff = (values.size() <= 1) ?  maxValidValue + 1 : Integer.MAX_VALUE;

                if (values.size() > 1) {
                    Collections.sort(values);
                    for (int i = 0; i < values.size() - 1; i++) {
                        int diff = values.get(i + 1) - values.get(i);
                        if (diff < minDiff) {
                            minDiff = diff;
                        }
                        int firstLastDiff = values.get(0) + (maxValidValue - minValidValue + 1) - values.get(values.size() - 1);

                        if (minDiff > firstLastDiff) {
                            minDiff = firstLastDiff;
                        }
                    }
                }
                if (values.size() > 0) {
                    value = Integer.toString(minDiff);
                }
            }
            validityPeriodEstimator.setFieldValue(field, value);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Set " + field + " to " + value);
            }
        }
        return ret;
    }

    private String getNormalizedValue(RangerValidityRecurrence recurrence, RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec field) {
        String ret = null;

        if (RangerValidityRecurrence.ValidityInterval.getValidityIntervalInMinutes(recurrence.getInterval()) > 0) {
            String noWhiteSpace = StringUtils.deleteWhitespace(recurrence.getSchedule().getFieldValue(field));
            String[] specs = StringUtils.split(noWhiteSpace, ",");

            List<String> values = new ArrayList<>();

            for (String spec : specs) {
                if (StringUtils.isNotBlank(spec)) {
                    values.add(spec);
                }
            }
            if (values.size() > 0) {
                Collections.sort(values);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < values.size(); i++) {
                    if (i != 0) {
                        sb.append(",");
                    }
                    sb.append(values.get(i));
                }
                ret = sb.toString();
            }
        }
        return ret;
    }

}
