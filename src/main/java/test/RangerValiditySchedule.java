package test;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.Date;

@JsonAutoDetect(fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)

public class RangerValiditySchedule {

    enum ScheduleFieldSpec {
        minute(0, 59, PERMITTED_SPECIAL_CHARACTERS_FOR_MINUTES),
        hour(0, 23, PERMITTED_SPECIAL_CHARACTERS),
        dayOfMonth(1, 31, PERMITTED_SPECIAL_CHARACTERS),
        dayOfWeek(1, 7, PERMITTED_SPECIAL_CHARACTERS),
        month(0, 11, PERMITTED_SPECIAL_CHARACTERS),
        year(2017, Integer.MAX_VALUE, PERMITTED_SPECIAL_CHARACTERS),
        ;

        final int minimum;
        final int maximum;
        final String specialChars;

        ScheduleFieldSpec(int minimum, int maximum, String specialChars) {
            this.minimum = minimum;
            this.maximum = maximum;
            this.specialChars = specialChars;
        }
    }

    static final String PERMITTED_SPECIAL_CHARACTERS = "*,-";
    static final String PERMITTED_SPECIAL_CHARACTERS_FOR_MINUTES = ",";
    public static final String WILDCARD = "*";

    private String minute;
    private String hour;
    private String dayOfMonth;
    private String dayOfWeek;
    private String month;
    private String year;
    private long startTime;
    private long endTime;
    private int interval;

    public RangerValiditySchedule(String minute, String hour, String dayOfMonth, String dayOfWeek, String month, String year,
                                  long startTime, long endTime, int interval) {
        setMinute(minute);
        setHour(hour);
        setDayOfMonth(dayOfMonth);
        setDayOfWeek(dayOfWeek);
        setMonth(month);
        setYear(year);
        setStartTime(startTime);
        setEndTime(endTime);
        setInterval(interval);
    }

    RangerValiditySchedule() {
        this(null, null, null, null, null, null, 0, 0, 0);
    }

    public String getMinute() { return minute;}
    public String getHour() { return hour;}
    public String getDayOfMonth() { return dayOfMonth;}
    public String getDayOfWeek() { return dayOfWeek;}
    public String getMonth() { return month;}
    public String getYear() { return year;}
    public long getStartTime() { return startTime;}
    public long getEndTime() { return endTime;}
    public int getInterval() { return interval;}

    public void setMinute(String minute) { this.minute = minute;}
    public void setHour(String hour) { this.hour = hour;}
    public void setDayOfMonth(String dayOfMonth) { this.dayOfMonth = dayOfMonth;}
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek;}
    public void setMonth(String month) { this.month = month;}
    public void setYear(String year) { this.year = year;}
    public void setStartTime(long startTime) { this.startTime = startTime;}
    public void setEndTime(long endTime) { this.endTime = endTime;}
    public void setInterval(int interval) { this.interval = interval;}

    public void setFieldValue(ScheduleFieldSpec field, String value) {
        switch (field) {
            case minute:
                setMinute(value);
                break;
            case hour:
                setHour(value);
                break;
            case dayOfMonth:
                setDayOfMonth(value);
                break;
            case dayOfWeek:
                setDayOfWeek(value);
                break;
            case month:
                setMonth(value);
                break;
            case year:
                setYear(value);
                break;
            default:
                break;
        }
    }

    public String getFieldValue(ScheduleFieldSpec field) {
        switch (field) {
            case minute:
                return getMinute();
            case hour:
                return getHour();
            case dayOfMonth:
                return getDayOfMonth();
            case dayOfWeek:
                return getDayOfWeek();
            case month:
                return getMonth();
            case year:
                return getYear();
            default:
                return null;
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("RangerValiditySchedule={");
        sb.append(" minute=").append(minute);
        sb.append(", hour=").append(hour);
        sb.append(", dayOfMonth=").append(dayOfMonth);
        sb.append(", dayOfWeek=").append(dayOfWeek);
        sb.append(", month=").append(month);
        sb.append(", year=").append(year);
        sb.append(", startTime=").append(new Date(startTime));
        sb.append(", endTime=").append(new Date(endTime));
        sb.append(", interval-in-minutes=").append(interval);
        sb.append(" }");
        return sb.toString();
    }

}