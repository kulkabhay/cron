package test;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ranger.plugin.util.RangerPerfTracer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class RangerValidityScheduleEvaluator {

    private static final Log LOG = LogFactory.getLog(RangerValidityScheduleEvaluator.class);
    private static final Log PERF_LOG = LogFactory.getLog("test.perf.RangerValidityScheduleEvaluator");

    private List<ScheduledTimeMatcher> minutes = new ArrayList<>();
    private List<ScheduledTimeMatcher> hours = new ArrayList<>();
    private List<ScheduledTimeMatcher> daysOfMonth = new ArrayList<>();
    private List<ScheduledTimeMatcher> daysOfWeek = new ArrayList<>();
    private List<ScheduledTimeMatcher> months = new ArrayList<>();
    private List<ScheduledTimeMatcher> years = new ArrayList<>();

    private final RangerValiditySchedule validitySchedule;
    private final int intervalInMinutes;

    public RangerValidityScheduleEvaluator(RangerValiditySchedule validitySchedule) {

        this.validitySchedule = validitySchedule;

        intervalInMinutes = validitySchedule.getValidityIntervalInMinutes();
        if (intervalInMinutes > 0) {
            addScheduledTime(validitySchedule.getMinute(), minutes);
            addScheduledTime(validitySchedule.getHour(), hours);
            addScheduledTime(validitySchedule.getDayOfMonth(), daysOfMonth);
            addScheduledTime(validitySchedule.getDayOfWeek(), daysOfWeek);
            addScheduledTime(validitySchedule.getMonth(), months);
            addScheduledTime(validitySchedule.getYear(), years);
        }
    }

    public boolean isApplicable(long accessTime) {
        boolean ret = false;
        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_LOG, "RangerValidityScheduleEvaluator.isApplicable(accessTime=" + accessTime + ")");
        }

        long startTimeInMSs = validitySchedule.getStartTime() == null ? 0 : validitySchedule.getStartTime().getTime();
        long endTimeInMSs = validitySchedule.getEndTime() == null ? 0 : validitySchedule.getEndTime().getTime();

        if (accessTime >= startTimeInMSs && accessTime <= endTimeInMSs) {
            if (intervalInMinutes > 0) { // recurring schedule

                Calendar now = new GregorianCalendar();
                now.setTime(new Date(accessTime));

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Access-Time:[" + now.getTime() + "]");
                }

                Calendar startOfInterval = getClosestPastEpoch(now);

                if (startOfInterval != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Start-of-Interval:[" + startOfInterval.getTime() + "]");
                    }

                    Calendar endOfInterval = (Calendar) startOfInterval.clone();
                    endOfInterval.add(Calendar.MINUTE, validitySchedule.getValidityInterval().getMinutes());
                    endOfInterval.add(Calendar.HOUR, validitySchedule.getValidityInterval().getHours());
                    endOfInterval.add(Calendar.DAY_OF_MONTH, validitySchedule.getValidityInterval().getDays());

                    endOfInterval.getTime();    // for recomputation

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("End-of-Interval:[" + endOfInterval.getTime() + "]");
                    }
                    ret = startOfInterval.before(now) && endOfInterval.after(now);
                }

            } else {
                ret = true;
            }
        }
        RangerPerfTracer.logAlways(perf);
        return ret;
    }

    private void addScheduledTime(String str, List<ScheduledTimeMatcher> list) {
        if (StringUtils.isNotBlank(str)) {
            String[] specs = str.split(",");
            for (String spec : specs) {
                String[] range = spec.split("-");
                if (range.length == 1) {
                    if (StringUtils.equals(range[0], RangerValiditySchedule.WILDCARD)) {
                        list.clear();
                        list.add(new ScheduledTimeAlwaysMatcher());
                        break;
                    } else {
                        list.add(new ScheduledTimeExactMatcher(Integer.valueOf(range[0])));
                    }
                } else {
                    if (StringUtils.equals(range[0], RangerValiditySchedule.WILDCARD) || StringUtils.equals(range[1], RangerValiditySchedule.WILDCARD)) {
                        list.clear();
                        list.add(new ScheduledTimeAlwaysMatcher());
                        break;
                    } else {
                        list.add(new ScheduledTimeRangeMatcher(Integer.valueOf(range[0]), Integer.valueOf(range[1])));
                    }
                }
            }
            Collections.reverse(list);
        }

    }

    /*
    Given a Calendar object, get the closest, earlier Calendar object based on configured validity schedules.
    Returns - a valid Calendar object. Throws exception if any errors during processing or no suitable Calendar object is found.
    Description - Typically, a caller will call this with Calendar constructed with current time, and use returned object
                along with specified interval to ensure that next schedule time is after the input Calendar.
    Algorithm -   This involves doing a Calendar arithmetic (subtraction) with borrow. The tricky parts are ensuring that
                  Calendar arithmetic yields a valid Calendar object.
                    - Start with minutes, and then hours.
                    - Must make sure that the later of the two Calendars - one computed with dayOfMonth, another computed with
                      dayOfWeek - is picked
                    - For dayOfMonth calculation, consider that months have different number of days
    */

    private class ValueWithBorrow {
        int value;
        boolean borrow;

        ValueWithBorrow() {}

        ValueWithBorrow(int value) {
            this(value, false);
        }

        ValueWithBorrow(int value, boolean borrow) {
            this.value = value;
            this.borrow = borrow;
        }
        void setValue(int value) { this.value = value;}
        void setBorrow(boolean borrow) { this.borrow = borrow; }
        int getValue() { return value;}
        boolean getBorrow() { return borrow;}

        @Override
        public String toString() {
            return "value=" + value + ", borrow=" + borrow;
        }
    }

    private Calendar getClosestPastEpoch(Calendar current) {
        Calendar ret = null;

        try {
            ValueWithBorrow input = new ValueWithBorrow();

            input.setValue(current.get(Calendar.MINUTE));
            input.setBorrow(false);
            ValueWithBorrow closestMinute = getPastFieldValueWithBorrow(RangerValiditySchedule.ScheduleFieldSpec.minute, minutes, input);

            input.setValue(current.get(Calendar.HOUR_OF_DAY));
            input.setBorrow(closestMinute.borrow);
            ValueWithBorrow closestHour = getPastFieldValueWithBorrow(RangerValiditySchedule.ScheduleFieldSpec.hour, hours, input);

            Calendar dayOfMonthCalendar = getClosestDayOfMonth(current, closestMinute, closestHour);

            Calendar dayOfWeekCalendar = getClosestDayOfWeek(current, closestMinute, closestHour);

            ret = getEarlierCalendar(dayOfMonthCalendar, dayOfWeekCalendar);

            if (LOG.isDebugEnabled()) {
                LOG.debug("ClosestPastEpoch:[" + (ret != null ? ret.getTime() : ret) + "]");
            }

        } catch (Exception e) {
            LOG.error("Could not find ClosestPastEpoch, Exception=", e);
        }
        return ret;
    }

    private Calendar getClosestDayOfMonth(Calendar current, ValueWithBorrow closestMinute, ValueWithBorrow closestHour) throws Exception {
        Calendar ret = null;
        if (StringUtils.isNotBlank(validitySchedule.getDayOfMonth())) {
            int initialDayOfMonth = current.get(Calendar.DAY_OF_MONTH);

            int currentDayOfMonth = initialDayOfMonth, currentMonth = current.get(Calendar.MONTH), currentYear = current.get(Calendar.YEAR);
            int maximumDaysInPreviousMonth = getMaximumValForPreviousMonth(current);

            if (closestHour.borrow) {
                initialDayOfMonth--;
                Calendar dayOfMonthCalc = (GregorianCalendar) current.clone();
                dayOfMonthCalc.add(Calendar.DAY_OF_MONTH, -1);
                dayOfMonthCalc.getTime();
                int previousDayOfMonth = dayOfMonthCalc.get(Calendar.DAY_OF_MONTH);
                if (initialDayOfMonth < previousDayOfMonth) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Need to borrow from previous month, initialDayOfMonth:[" + initialDayOfMonth + "], previousDayOfMonth:[" + previousDayOfMonth + "], dayOfMonthCalc:[" + dayOfMonthCalc.getTime() + "]");
                    }
                    currentDayOfMonth = previousDayOfMonth;
                    currentMonth = dayOfMonthCalc.get(Calendar.MONTH);
                    currentYear = dayOfMonthCalc.get(Calendar.YEAR);
                    maximumDaysInPreviousMonth = getMaximumValForPreviousMonth(dayOfMonthCalc);
                } else if (initialDayOfMonth == previousDayOfMonth) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("No need to borrow from previous month, initialDayOfMonth:[" + initialDayOfMonth + "], previousDayOfMonth:[" + previousDayOfMonth + "]");
                    }
                } else {
                    LOG.error("Should not get here, initialDayOfMonth:[" + initialDayOfMonth + "], previousDayOfMonth:[" + previousDayOfMonth + "]");
                    throw new Exception("Should not get here, initialDayOfMonth:[" + initialDayOfMonth + "], previousDayOfMonth:[" + previousDayOfMonth + "]");
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("currentDayOfMonth:[" + currentDayOfMonth + "], maximumDaysInPreviourMonth:[" + maximumDaysInPreviousMonth + "]");
            }
            ValueWithBorrow input = new ValueWithBorrow();
            input.setValue(currentDayOfMonth);
            input.setBorrow(false);
            ValueWithBorrow closestDayOfMonth = null;
            do {
                int i = 0;
                try {
                    closestDayOfMonth = getPastFieldValueWithBorrow(RangerValiditySchedule.ScheduleFieldSpec.dayOfMonth, daysOfMonth, input, maximumDaysInPreviousMonth);
                } catch (Exception e) {
                    i++;
                    Calendar c = (GregorianCalendar) current.clone();
                    c.set(Calendar.YEAR, currentYear);
                    c.set(Calendar.MONTH, currentMonth);
                    c.set(Calendar.DAY_OF_MONTH, currentDayOfMonth);
                    c.add(Calendar.MONTH, -i);
                    c.getTime();
                    currentMonth = c.get(Calendar.MONTH);
                    currentYear = c.get(Calendar.YEAR);
                    currentDayOfMonth = c.get(Calendar.DAY_OF_MONTH);
                    maximumDaysInPreviousMonth = getMaximumValForPreviousMonth(c);
                    input.setValue(currentDayOfMonth);
                    input.setBorrow(false);
                }
            } while (closestDayOfMonth == null);

            // Build calendar for dayOfMonth
            ret = new GregorianCalendar();
            ret.set(Calendar.DAY_OF_MONTH, closestDayOfMonth.value);
            ret.set(Calendar.HOUR_OF_DAY, closestHour.value);
            ret.set(Calendar.MINUTE, closestMinute.value);

            ret.set(Calendar.YEAR, currentYear);

            if (closestDayOfMonth.borrow) {
                ret.add(Calendar.MONTH, currentMonth - 1);
            } else {
                ret.set(Calendar.MONTH, currentMonth);
            }
            ret.getTime(); // For recomputation

            if (LOG.isDebugEnabled()) {
                LOG.debug("Best guess using DAY_OF_MONTH:[" + ret.getTime() + "]");
            }
        }
        return ret;
    }

    private Calendar getClosestDayOfWeek(Calendar current, ValueWithBorrow closestMinute, ValueWithBorrow closestHour) throws Exception {
        Calendar ret = null;
        if (StringUtils.isNotBlank(validitySchedule.getDayOfWeek())) {
            ValueWithBorrow input = new ValueWithBorrow();

            input.setValue(current.get(Calendar.DAY_OF_WEEK));
            input.setBorrow(closestHour.borrow);


            ValueWithBorrow closestDayOfWeek = getPastFieldValueWithBorrow(RangerValiditySchedule.ScheduleFieldSpec.dayOfWeek, daysOfWeek, input);

            int daysToGoback = closestHour.borrow ? 1 : 0;
            int range = RangerValiditySchedule.ScheduleFieldSpec.dayOfWeek.maximum - RangerValiditySchedule.ScheduleFieldSpec.dayOfWeek.minimum + 1;

            if (closestDayOfWeek.borrow) {
                if (input.value - closestDayOfWeek.value != daysToGoback) {
                    daysToGoback = range + input.value - closestDayOfWeek.value;
                }
            } else {
                daysToGoback =  input.value - closestDayOfWeek.value;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Need to go back [" + daysToGoback + "] days to match dayOfWeek");
            }

            ret = (GregorianCalendar) current.clone();
            ret.set(Calendar.MINUTE, closestMinute.value);
            ret.set(Calendar.HOUR_OF_DAY, closestHour.value);
            ret.add(Calendar.DAY_OF_MONTH, (0 - daysToGoback));

            ret.getTime();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Best guess using DAY_OF_WEEK:[" + ret.getTime() + "]");
            }
        }
        return ret;

    }

    private int getMaximumValForPreviousMonth(Calendar current) {
        Calendar cal = (Calendar)current.clone();
        cal.add(Calendar.MONTH, -1);
        cal.getTime(); // For recomputation

        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private Calendar getEarlierCalendar(Calendar dayOfMonthCalendar, Calendar dayOfWeekCalendar) throws Exception {

        Calendar withDayOfMonth = fillOutCalendar(dayOfMonthCalendar);
        if (LOG.isDebugEnabled()) {
            LOG.debug("dayOfMonthCalendar:[" + (withDayOfMonth != null ? withDayOfMonth.getTime() : withDayOfMonth) + "]");
        }

        Calendar withDayOfWeek = fillOutCalendar(dayOfWeekCalendar);
        if (LOG.isDebugEnabled()) {
            LOG.debug("dayOfWeekCalendar:[" + (withDayOfWeek != null ? withDayOfWeek.getTime() : withDayOfWeek) + "]");
        }

        if (withDayOfMonth != null && withDayOfWeek != null) {
            return withDayOfMonth.after(withDayOfWeek) ? withDayOfMonth : withDayOfWeek;
        } else if (withDayOfMonth == null) {
            return withDayOfWeek;
        } else {
            return withDayOfMonth;
        }
    }

    private Calendar fillOutCalendar(Calendar calendar) throws Exception {
        Calendar ret = null;

        if (calendar != null) {
            ValueWithBorrow input = new ValueWithBorrow(calendar.get(Calendar.MONTH));
            ValueWithBorrow closestMonth = getPastFieldValueWithBorrow(RangerValiditySchedule.ScheduleFieldSpec.month, months, input);

            input.setValue(calendar.get(Calendar.YEAR));
            input.setBorrow(closestMonth.borrow);
            ValueWithBorrow closestYear = getPastFieldValueWithBorrow(RangerValiditySchedule.ScheduleFieldSpec.year, years, input);

            // Build calendar
            ret = (Calendar) calendar.clone();
            ret.set(Calendar.YEAR, closestYear.value);
            ret.set(Calendar.MONTH, closestMonth.value);
            ret.set(Calendar.SECOND, 0);

            ret.getTime(); // for recomputation
            if (LOG.isDebugEnabled()) {
                LOG.debug("Filled-out-Calendar:[" + ret.getTime() + "]");
            }
        }
        return ret;
    }

    private ValueWithBorrow getPastFieldValueWithBorrow(RangerValiditySchedule.ScheduleFieldSpec fieldSpec, List<ScheduledTimeMatcher> searchList, ValueWithBorrow input) throws Exception {
        return getPastFieldValueWithBorrow(fieldSpec, searchList, input, fieldSpec.maximum);
    }

    private ValueWithBorrow getPastFieldValueWithBorrow(RangerValiditySchedule.ScheduleFieldSpec fieldSpec, List<ScheduledTimeMatcher> searchList, ValueWithBorrow input, int maximum) throws Exception {

        ValueWithBorrow ret;
        boolean borrow = false;

        int value = input.value - (input.borrow ? 1 : 0);

        if (CollectionUtils.isNotEmpty(searchList)) {
            int range = fieldSpec.maximum - fieldSpec.minimum + 1;

            for (int i = 0; i < range; i++, value--) {
                if (value < fieldSpec.minimum) {
                    value = maximum;
                    borrow = true;
                }
                for (ScheduledTimeMatcher time : searchList) {
                    if (time.isMatch(value)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Found match in field:[" + fieldSpec + "], value:[" + value + "], borrow:[" + borrow + "], maximum:[" + maximum + "]");
                        }
                        return new ValueWithBorrow(value, borrow);
                    }
                }
            }
            // Not found
            throw new Exception("No match found in field:[" + fieldSpec + "] for [input=" + input + "]");
        } else {
            if (value < fieldSpec.minimum) {
                value = maximum;
            }
            ret = new ValueWithBorrow(value, borrow);
        }
        return ret;
    }
}
