package test;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class RangerValidityScheduleValidator {

    public enum Action {
        CREATE, UPDATE, DELETE
    }

    final private RangerValiditySchedule validitySchedule;

    private RangerValiditySchedule validityPeriodEstimator;

    RangerValidityScheduleValidator(RangerValiditySchedule validitySchedule) {
        this.validitySchedule = validitySchedule;
    }

    public RangerValiditySchedule validate(Action action, List<ValidationFailureDetails> validationFailures) {
        RangerValiditySchedule ret = null;
        validityPeriodEstimator = new RangerValiditySchedule();

        boolean isValid = validateTimeRangeSpec(action, validationFailures);
        if (isValid) {
            System.out.println("validityPeriodEstimator:[" + validityPeriodEstimator + "]");
            ret = getNormalizedValiditySchedule();
        }
        return ret;
    }

    private boolean validateTimeRangeSpec(Action action, List<ValidationFailureDetails> validationFailures) {
        boolean ret = false;

        long currentTime = new Date().getTime();

        if (validitySchedule.getInterval() < 0) {
            validationFailures.add(new ValidationFailureDetails(0, "interval", "", false, true, false, "interval less than 0"));
        } else {
            if (validitySchedule.getStartTime() >= validitySchedule.getEndTime()) {
                validationFailures.add(new ValidationFailureDetails(0, "startTime", "", false, true, false, "startTime later than endTime"));
            } else if (action == Action.CREATE && validitySchedule.getEndTime() <= currentTime) {
                validationFailures.add(new ValidationFailureDetails(0, "endTime", "", false, true, false, "endTime earlier than current time"));
            } else {
                if (validitySchedule.getInterval() > 0) {
                    ret = validateFieldSpec(RangerValiditySchedule.ScheduleFieldSpec.minute, validationFailures);
                    ret = validateFieldSpec(RangerValiditySchedule.ScheduleFieldSpec.hour, validationFailures) && ret;
                    ret = validateFieldSpec(RangerValiditySchedule.ScheduleFieldSpec.dayOfMonth, validationFailures) && ret;
                    ret = validateFieldSpec(RangerValiditySchedule.ScheduleFieldSpec.dayOfWeek, validationFailures) && ret;
                    ret = validateFieldSpec(RangerValiditySchedule.ScheduleFieldSpec.month, validationFailures) && ret;
                    ret = validateFieldSpec(RangerValiditySchedule.ScheduleFieldSpec.year, validationFailures) && ret;
                    ret = ret && validateInterval(validationFailures);
                } else {
                    ret = true;
                }
            }
        }

        return ret;
    }

    private RangerValiditySchedule getNormalizedValiditySchedule() {
        return new RangerValiditySchedule(getNormalizedValue(RangerValiditySchedule.ScheduleFieldSpec.minute), getNormalizedValue(RangerValiditySchedule.ScheduleFieldSpec.hour),
                getNormalizedValue(RangerValiditySchedule.ScheduleFieldSpec.dayOfMonth), getNormalizedValue(RangerValiditySchedule.ScheduleFieldSpec.dayOfWeek),
                getNormalizedValue(RangerValiditySchedule.ScheduleFieldSpec.month), getNormalizedValue(RangerValiditySchedule.ScheduleFieldSpec.year),
                validitySchedule.getStartTime(), validitySchedule.getEndTime(), validitySchedule.getInterval());

    }

    private boolean validateFieldSpec(RangerValiditySchedule.ScheduleFieldSpec field, List<ValidationFailureDetails> validationFailures) {
        boolean ret = true;

        String fieldValue = validitySchedule.getFieldValue(field);
        if (StringUtils.isNotBlank(fieldValue)) {
            ret = validateCharacters(fieldValue, field.specialChars);

            if (!ret) {
                validationFailures.add(new ValidationFailureDetails(0, field.toString(), "", false, true, false, "invalid character(s)"));
            } else {
                ret = validateRanges(field, field.minimum, field.maximum, validationFailures);
            }
        } else {
            validationFailures.add(new ValidationFailureDetails(0, field.toString(), "", false, true, false, "No value specified"));
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

    private boolean validateInterval(List<ValidationFailureDetails> validationFailures) {
        boolean ret = true;

        if (!validationFailures.isEmpty() || validityPeriodEstimator == null) {
            ret = false;
        } else {
            int minSchedulingInterval = 1; // In minutes

            String minutes = validityPeriodEstimator.getFieldValue(RangerValiditySchedule.ScheduleFieldSpec.minute);
            if (!StringUtils.equals(minutes, RangerValiditySchedule.WILDCARD)) {
                minSchedulingInterval = Integer.valueOf(minutes);

                if (minSchedulingInterval == RangerValiditySchedule.ScheduleFieldSpec.minute.maximum + 1) {
                    String hours = validityPeriodEstimator.getFieldValue(RangerValiditySchedule.ScheduleFieldSpec.hour);
                    if (!StringUtils.equals(hours, RangerValiditySchedule.WILDCARD)) {
                        int hour = Integer.valueOf(hours);
                        minSchedulingInterval = hour * (RangerValiditySchedule.ScheduleFieldSpec.minute.maximum+1);

                        if (hour == RangerValiditySchedule.ScheduleFieldSpec.hour.maximum + 1) {
                            String dayOfMonths = validityPeriodEstimator.getFieldValue(RangerValiditySchedule.ScheduleFieldSpec.dayOfMonth);
                            String dayOfWeeks = validityPeriodEstimator.getFieldValue(RangerValiditySchedule.ScheduleFieldSpec.dayOfWeek);
                            if (StringUtils.isBlank(dayOfMonths)) {
                                dayOfMonths = RangerValiditySchedule.WILDCARD;
                            }
                            if (StringUtils.isBlank(dayOfWeeks)) {
                                dayOfWeeks = RangerValiditySchedule.WILDCARD;
                            }
                            if (!StringUtils.equals(dayOfMonths, RangerValiditySchedule.WILDCARD) && !StringUtils.equals(dayOfWeeks, RangerValiditySchedule.WILDCARD)) {
                                int dayOfMonth = Integer.valueOf(dayOfMonths);
                                int dayOfWeek = Integer.valueOf(dayOfWeeks);
                                minSchedulingInterval = dayOfWeek*(RangerValiditySchedule.ScheduleFieldSpec.hour.maximum+1)*(RangerValiditySchedule.ScheduleFieldSpec.minute.maximum+1);

                                if (dayOfMonth == (RangerValiditySchedule.ScheduleFieldSpec.dayOfMonth.maximum+1) && dayOfWeek == (RangerValiditySchedule.ScheduleFieldSpec.dayOfWeek.maximum+1)) {
                                    String months = validityPeriodEstimator.getFieldValue(RangerValiditySchedule.ScheduleFieldSpec.month);
                                    if (!StringUtils.equals(months, RangerValiditySchedule.WILDCARD)) {
                                        int month = Integer.valueOf(months);
                                        minSchedulingInterval = month * 28 * (RangerValiditySchedule.ScheduleFieldSpec.hour.maximum + 1) * (RangerValiditySchedule.ScheduleFieldSpec.minute.maximum + 1);

                                        if (month == RangerValiditySchedule.ScheduleFieldSpec.month.maximum + 1) {
                                            // Maximum interval is 1 year
                                            minSchedulingInterval = 365 * (RangerValiditySchedule.ScheduleFieldSpec.hour.maximum + 1) * (RangerValiditySchedule.ScheduleFieldSpec.minute.maximum + 1);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (validitySchedule.getInterval() > minSchedulingInterval) {
                validationFailures.add(new ValidationFailureDetails(0, "interval", "", false, true, false, "interval longer than specified interval"));

                ret = false;
            }
        }
        return ret;
    }

    private boolean validateRanges(RangerValiditySchedule.ScheduleFieldSpec field, int minValidValue, int maxValidValue, List<ValidationFailureDetails> validationFailures) {
        boolean ret = true;

        String value = null;
        String fieldName = field.toString();

        String noWhiteSpace = StringUtils.deleteWhitespace(validitySchedule.getFieldValue(field));
        String[] specs = StringUtils.split(noWhiteSpace, ",");
        class Range {
            int lower;
            int upper;
            Range(int lower, int upper) {
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
        int numOfRanges = 0;

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
                        if (!StringUtils.equals(ranges[0], RangerValiditySchedule.WILDCARD)) {
                            val1 = Integer.valueOf(ranges[0]);
                            if (val1 < minValidValue || val1 > maxValidValue) {
                                validationFailures.add(new ValidationFailureDetails(0, fieldName, "", false, true, false, "incorrect lower range value"));
                                ret = false;
                            }
                        } else {
                            value = RangerValiditySchedule.WILDCARD;
                        }
                        if (!StringUtils.equals(ranges[1], RangerValiditySchedule.WILDCARD)) {
                            val2 = Integer.valueOf(ranges[1]);
                            if (val1 < minValidValue || val2 > maxValidValue) {
                                validationFailures.add(new ValidationFailureDetails(0, fieldName, "", false, true, false, "incorrect upper range value"));
                                ret = false;
                            }
                        } else {
                            value = RangerValiditySchedule.WILDCARD;
                        }
                        if (ret) {
                            if (val1 >= val2) {
                                validationFailures.add(new ValidationFailureDetails(0, fieldName, "", false, true, false, "incorrect range"));
                                ret = false;
                            } else {
                                value = RangerValiditySchedule.WILDCARD;
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
                        if (!StringUtils.equals(ranges[0], RangerValiditySchedule.WILDCARD)) {
                            int val = Integer.valueOf(ranges[0]);
                            if (val < minValidValue || val > maxValidValue) {
                                validationFailures.add(new ValidationFailureDetails(0, fieldName, "", false, true, false, "incorrect value"));
                                ret = false;
                            } else {
                                if (!StringUtils.equals(value, RangerValiditySchedule.WILDCARD)) {
                                    values.add(Integer.valueOf(ranges[0]));
                                }
                            }
                        } else {
                            value = RangerValiditySchedule.WILDCARD;
                        }
                    } else {
                        ret = false;
                    }
                }
            }
        }
        //if (ret) {
            if (CollectionUtils.isNotEmpty(rangeOfValues)) {
                Collections.sort(rangeOfValues, new RangeComparator());
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
            if (!StringUtils.equals(value, RangerValiditySchedule.WILDCARD)) {

                int minDiff = maxValidValue + 1;

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
                value = Integer.toString(minDiff);

            }
            validityPeriodEstimator.setFieldValue(field, value);
            System.out.println("Set " + field + " to " + value);
        }
        return ret;
    }

    private String getNormalizedValue(RangerValiditySchedule.ScheduleFieldSpec field) {
        String ret = null;

        if (validitySchedule.getInterval() > 0) {
            String noWhiteSpace = StringUtils.deleteWhitespace(validitySchedule.getFieldValue(field));
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
