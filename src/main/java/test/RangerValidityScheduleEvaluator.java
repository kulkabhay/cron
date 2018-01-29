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

    public RangerValidityScheduleEvaluator(RangerValiditySchedule entry) {

        this.validitySchedule = entry;

        intervalInMinutes = validitySchedule.getValidityIntervalInMinutes();
        if (intervalInMinutes > 0) {
            addScheduledTime(entry.getMinute(), minutes);
            addScheduledTime(entry.getHour(), hours);
            addScheduledTime(entry.getDayOfMonth(), daysOfMonth);
            addScheduledTime(entry.getDayOfWeek(), daysOfWeek);
            addScheduledTime(entry.getMonth(), months);
            addScheduledTime(entry.getYear(), years);
        }
    }

    public boolean isApplicable(long currentTime) {
        boolean ret = false;
        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_LOG, "RangerValidityScheduleEvaluator.isApplicable(currentTime=" + currentTime + ")");
            LOG.info("RangerValidityScheduleEvaluator.isApplicable(" + currentTime + ")");
        }


        long startTimeInMSs = validitySchedule.getStartTime() == null ? 0 : validitySchedule.getStartTime().getTime();
        long endTimeInMSs = validitySchedule.getEndTime() == null ? 0 : validitySchedule.getEndTime().getTime();

        if (currentTime >= startTimeInMSs && currentTime <= endTimeInMSs) {
            if (intervalInMinutes > 0) { // recurring schedule

                Calendar now = new GregorianCalendar();
                now.setTime(new Date(currentTime));

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Current-Time:[" + now.getTime() + "]");
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

                    //endOfInterval.add(Calendar.MINUTE, intervalInMinutes);
                    endOfInterval.getTime();    // for recomputation
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("End-of-Interval:[" + endOfInterval.getTime() + "]");
                    }
                    ret = endOfInterval.after(now);
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
    private Calendar getClosestPastEpoch(Calendar current) {
        Calendar ret = null;

        try {
            boolean borrow;

            int minute = getPastFieldValue(RangerValiditySchedule.ScheduleFieldSpec.minute, minutes, current.get(Calendar.MINUTE));
            if (minute < RangerValiditySchedule.ScheduleFieldSpec.minute.minimum) {
                minute = RangerValiditySchedule.ScheduleFieldSpec.minute.maximum - RangerValiditySchedule.ScheduleFieldSpec.minute.minimum + 1 + minute;
                borrow = true;
            } else {
                borrow = false;
            }

            int hour = getPastFieldValue(RangerValiditySchedule.ScheduleFieldSpec.hour, hours, current.get(Calendar.HOUR_OF_DAY) - (borrow ? 1 : 0));
            if (hour < RangerValiditySchedule.ScheduleFieldSpec.hour.minimum) {
                hour = RangerValiditySchedule.ScheduleFieldSpec.hour.maximum - RangerValiditySchedule.ScheduleFieldSpec.hour.minimum + 1 + hour;
                borrow = true;
            } else {
                borrow = false;
            }

            int initialDay = current.get(Calendar.DAY_OF_MONTH);
            boolean borrowForMonth = false;
            if (borrow) {
                Calendar cal = new GregorianCalendar(current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH) - 1);
                cal.setLenient(false);

                for (int i = 1; true; ) {
                    try {
                        initialDay = cal.get(Calendar.DAY_OF_MONTH) - 1;
                        break;
                    } catch (Exception e) {
                        i++;
                        cal.set(Calendar.DAY_OF_MONTH, current.get(Calendar.DAY_OF_MONTH) - i);
                        borrowForMonth = true;
                    }
                }
            }

            int maximumDaysInPreviousMonth = getMaximumValForPreviousMonth(current);

            boolean borrowForDayOfMonth;
            int dayOfMonth = getPastFieldValue(RangerValiditySchedule.ScheduleFieldSpec.dayOfMonth, daysOfMonth, initialDay, maximumDaysInPreviousMonth);
            if (dayOfMonth < RangerValiditySchedule.ScheduleFieldSpec.dayOfMonth.minimum) {
                dayOfMonth = maximumDaysInPreviousMonth - RangerValiditySchedule.ScheduleFieldSpec.dayOfMonth.minimum + 1 + dayOfMonth;
                borrowForDayOfMonth = true;
            } else {
                borrowForDayOfMonth = false;
            }

            // Build calendar for dayOfMonth
            Calendar dayOfMonthCalendar = new GregorianCalendar(current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH));
            dayOfMonthCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            if (borrowForMonth && borrowForDayOfMonth) {
                dayOfMonthCalendar.set(Calendar.MONTH, current.get(Calendar.MONTH) - 2);
            } else if (borrowForMonth || borrowForDayOfMonth) {
                dayOfMonthCalendar.set(Calendar.MONTH, current.get(Calendar.MONTH) - 1);
            }
            dayOfMonthCalendar.set(Calendar.HOUR_OF_DAY, hour);
            dayOfMonthCalendar.set(Calendar.MINUTE, minute);
            dayOfMonthCalendar.getTime(); // For recomputation

            int week = current.get(Calendar.WEEK_OF_YEAR);

            boolean borrowForDayOfWeek;
            int dayOfWeek = getPastFieldValue(RangerValiditySchedule.ScheduleFieldSpec.dayOfWeek, daysOfWeek, current.get(Calendar.DAY_OF_WEEK) - (borrow ? 1 : 0));
            if (dayOfWeek < RangerValiditySchedule.ScheduleFieldSpec.dayOfWeek.minimum) {
                dayOfWeek = RangerValiditySchedule.ScheduleFieldSpec.dayOfWeek.maximum - RangerValiditySchedule.ScheduleFieldSpec.dayOfWeek.minimum + 1 + dayOfWeek;
                borrowForDayOfWeek = true;
            } else {
                borrowForDayOfWeek = false;
            }
            int yearWithWeek = current.get(Calendar.YEAR);
            if (week == current.getGreatestMinimum(Calendar.WEEK_OF_YEAR) && borrowForDayOfWeek) {
                yearWithWeek--;
                week = current.getLeastMaximum(Calendar.WEEK_OF_YEAR);
            }
            // Build calendar for dayOfWeek
            Calendar dayOfWeekCalendar = new GregorianCalendar();
            dayOfWeekCalendar.set(Calendar.YEAR, yearWithWeek);
            dayOfWeekCalendar.set(Calendar.WEEK_OF_YEAR, week - (borrowForDayOfWeek ? 1 : 0));
            dayOfWeekCalendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
            dayOfWeekCalendar.set(Calendar.HOUR_OF_DAY, hour);
            dayOfWeekCalendar.set(Calendar.MINUTE, minute);

            dayOfWeekCalendar.getTime(); // For recomputation

            ret = getEarlierCalendar(dayOfMonthCalendar, dayOfWeekCalendar);

            if (LOG.isDebugEnabled()) {
                LOG.debug("ClosestPastEpoch:[" + ret.getTime() + "]");
            }

        } catch (Exception e) {
            LOG.error("Could not find ClosestPastEpoch", e);
        }
        return ret;
    }

    private int getPastFieldValue(RangerValiditySchedule.ScheduleFieldSpec fieldSpec, List<ScheduledTimeMatcher> searchList, int value) throws Exception {

        int ret = value;

        if (CollectionUtils.isNotEmpty(searchList)) {


            int range = fieldSpec.maximum - fieldSpec.minimum + 1;

            if (ret < fieldSpec.minimum) {
                ret = fieldSpec.maximum;
            }

            boolean found = false;
            boolean borrow = false;
            for (int i = 0; i < range; i++, ret--) {
                if (ret < fieldSpec.minimum) {
                    ret = fieldSpec.maximum;
                    borrow = true;
                }
                for (ScheduledTimeMatcher time : searchList) {
                    if (time.isMatch(ret)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Found match in field:[" + fieldSpec + "], value:[" + ret + "], borrow:[" + borrow + "]");
                        }
                        ret = borrow ? ret - range : ret;
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
            if (!found) {
                throw new Exception();
            }
        }
        return ret;

    }
    private int getPastFieldValue(RangerValiditySchedule.ScheduleFieldSpec fieldSpec, List<ScheduledTimeMatcher> searchList, int value, int maximum) throws Exception {

        int ret = value;

        if (CollectionUtils.isNotEmpty(searchList)) {

            int range = maximum - fieldSpec.minimum + 1;

            if (ret < fieldSpec.minimum) {
                ret = maximum;
            }

            boolean found = false;
            boolean borrow = false;
            for (int i = 0; i < range; i++, ret--) {
                if (ret < fieldSpec.minimum) {
                    ret = maximum;
                    borrow = true;
                }
                for (ScheduledTimeMatcher time : searchList) {
                    if (time.isMatch(ret)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Found match in field:[" + fieldSpec + "], value:[" + ret + "], borrow:[" + borrow + "], maximum:[" + maximum + "]");
                        }
                        ret = borrow ? ret - range : ret;
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
            if (!found) {
                throw new Exception();
            }
        }
        return ret;

    }
    private int getMaximumValForPreviousMonth(Calendar current) {
        Calendar cal = (Calendar)current.clone();
        cal.setLenient(false);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        int newMonth = month;
        int newYear = year;
        if (month == RangerValiditySchedule.ScheduleFieldSpec.month.minimum) {
            newMonth = RangerValiditySchedule.ScheduleFieldSpec.month.maximum;
            newYear--;
        } else {
            newMonth--;
        }
        cal.set(Calendar.MONTH, newMonth);
        cal.set(Calendar.YEAR, newYear);
        cal.getTime(); // For recomputation

        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private Calendar getEarlierCalendar(Calendar dayOfMonthCalendar, Calendar dayOfWeekCalendar) throws Exception {

        if (LOG.isDebugEnabled()) {
            LOG.debug("dayOfMonthCalendar:[" + dayOfMonthCalendar.getTime() + "]");
        }
        Calendar withDayOfMonth = fillOutCalendar(dayOfMonthCalendar);

        if (LOG.isDebugEnabled()) {
            LOG.debug("dayOfWeekCalendar:[" + dayOfWeekCalendar.getTime() + "]");
        }
        Calendar withDayOfWeek = fillOutCalendar(dayOfWeekCalendar);

        return withDayOfMonth.after(withDayOfWeek) ? withDayOfMonth : withDayOfWeek;
    }

    private Calendar fillOutCalendar(Calendar calendar) throws Exception {
        Calendar ret;

        boolean borrow;

        int month = getPastFieldValue(RangerValiditySchedule.ScheduleFieldSpec.month, months, calendar.get(Calendar.MONTH));
        if (month < RangerValiditySchedule.ScheduleFieldSpec.month.minimum) {
            month = RangerValiditySchedule.ScheduleFieldSpec.month.maximum - RangerValiditySchedule.ScheduleFieldSpec.month.minimum + 1 + month;
            borrow = true;
        } else {
            borrow = false;
        }

        int year = getPastFieldValue(RangerValiditySchedule.ScheduleFieldSpec.year, years, calendar.get(Calendar.YEAR) - (borrow ? 1 : 0));
        if (year < RangerValiditySchedule.ScheduleFieldSpec.year.minimum) {
            year = RangerValiditySchedule.ScheduleFieldSpec.year.maximum - RangerValiditySchedule.ScheduleFieldSpec.year.minimum + 1 + year;
        }

        // Build calendar
        ret = (Calendar)calendar.clone();
        ret.set(Calendar.YEAR, year);
        ret.set(Calendar.MONTH, month);
        ret.set(Calendar.SECOND, 0);

        ret.getTime(); // for recomputation
        if (LOG.isDebugEnabled()) {
            LOG.debug("Filled-out-Calendar:[" + ret.getTime() + "]");
        }

        return ret;
    }
}
