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

    public enum ScheduleFieldSpec {
        minute(0, 59, PERMITTED_SPECIAL_CHARACTERS_FOR_MINUTES),
        hour(0, 23, PERMITTED_SPECIAL_CHARACTERS),
        dayOfMonth(1, 31, PERMITTED_SPECIAL_CHARACTERS),
        dayOfWeek(1, 7, PERMITTED_SPECIAL_CHARACTERS),
        month(0, 11, PERMITTED_SPECIAL_CHARACTERS),
        year(2017, Integer.MAX_VALUE, PERMITTED_SPECIAL_CHARACTERS),
        ;

        public final int minimum;
        public final int maximum;
        public final String specialChars;

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
    private RangerValidityInterval validityInterval;

    public RangerValiditySchedule(String minute, String hour, String dayOfMonth, String dayOfWeek, String month, String year,
                                  long startTime, long endTime, RangerValidityInterval validityInterval) {
        setMinute(minute);
        setHour(hour);
        setDayOfMonth(dayOfMonth);
        setDayOfWeek(dayOfWeek);
        setMonth(month);
        setYear(year);
        setStartTime(startTime);
        setEndTime(endTime);
        setValidityInterval(validityInterval);
    }

    public RangerValiditySchedule() {
        this(null, null, null, null, null, null, 0, 0, null);
    }

    public String getMinute() { return minute;}
    public String getHour() { return hour;}
    public String getDayOfMonth() { return dayOfMonth;}
    public String getDayOfWeek() { return dayOfWeek;}
    public String getMonth() { return month;}
    public String getYear() { return year;}
    public long getStartTime() { return startTime;}
    public long getEndTime() { return endTime;}
    public RangerValidityInterval getValidityInterval() { return validityInterval;}

    public void setMinute(String minute) { this.minute = minute;}
    public void setHour(String hour) { this.hour = hour;}
    public void setDayOfMonth(String dayOfMonth) { this.dayOfMonth = dayOfMonth;}
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek;}
    public void setMonth(String month) { this.month = month;}
    public void setYear(String year) { this.year = year;}
    public void setStartTime(long startTime) { this.startTime = startTime;}
    public void setEndTime(long endTime) { this.endTime = endTime;}
    public void setValidityInterval(RangerValidityInterval validityInterval) { this.validityInterval = validityInterval;}

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

    public int getValidityIntervalInMinutes() {
        RangerValidityInterval validityInterval = getValidityInterval();
        return validityInterval != null ?
                (validityInterval.getDays()*24 + validityInterval.getHours())*60 + validityInterval.getMinutes() : 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RangerValiditySchedule={");
        sb.append(" minute=").append(minute);
        sb.append(", hour=").append(hour);
        sb.append(", dayOfMonth=").append(dayOfMonth);
        sb.append(", dayOfWeek=").append(dayOfWeek);
        sb.append(", month=").append(month);
        sb.append(", year=").append(year);
        sb.append(", startTime=").append(new Date(startTime));
        sb.append(", endTime=").append(new Date(endTime));
        sb.append(", validityInterval=").append(validityInterval);
        sb.append(" }");
        return sb.toString();
    }

    @JsonAutoDetect(fieldVisibility=Visibility.ANY)
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public class RangerValidityInterval {
        private final int days;
        private final int hours;
        private final int minutes;

        public RangerValidityInterval() {
            this.days = 0;
            this.hours = 0;
            this.minutes = 0;
        }

        public RangerValidityInterval(int days, int hours, int minutes) {
            this.days = days;
            this.hours = hours;
            this.minutes = minutes;
        }

        public int getDays() { return days; }
        public int getHours() { return hours; }
        public int getMinutes() { return minutes; }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("RangerValidityInterval={");
            sb.append("days=").append(days);
            sb.append(", hours=").append(hours);
            sb.append(", minutes=").append(minutes);
            sb.append(" }");
            return sb.toString();
        }
    }


}
