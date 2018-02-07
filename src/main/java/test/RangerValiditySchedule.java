package test;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@JsonAutoDetect(fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)

public class RangerValiditySchedule {

    private static final Log LOG = LogFactory.getLog(RangerValiditySchedule.class);

    public static final String VALIDITY_SCHEDULE_DATE_STRING_SPECIFICATION = "yyyyMMdd-HH:mm";
    private static TimeZone defaultTZ = TimeZone.getDefault();

    public static long getAdjustedTime(long localTime, TimeZone timeZone) {
        long ret = localTime;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Input:[" + new Date(localTime) + ", target-timezone" + timeZone + "], default-timezone:[" + defaultTZ + "]");
        }

        if (!defaultTZ.equals(timeZone)) {

            int offsetFromTarget = timeZone.getOffset(localTime);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Offset of target-timezone :[" + offsetFromTarget + "]");
            }

            int offsetFromDefault = defaultTZ.getOffset(localTime);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Offset of default-timezone :[" + offsetFromDefault + "]");
            }

            ret += (offsetFromTarget - offsetFromDefault);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Output:[" + new Date(ret) + "]");
        }

        return ret;
    }

    private String startTime;
    private String endTime;
    private String timeZone;

    private List<RangerValidityRecurrence> recurrences;

    public RangerValiditySchedule(String timeZone, String startTime, String endTime, List<RangerValidityRecurrence> recurrences) {
        setTimeZone(timeZone);
        setStartTime(startTime);
        setEndTime(endTime);
        setRecurrences(recurrences);
    }

    public RangerValiditySchedule() {
        this(null, null, null, null);
    }

    public String getTimeZone() { return timeZone; }
    public String getStartTime() { return startTime;}
    public String getEndTime() { return endTime;}
    public List<RangerValidityRecurrence> getRecurrences() { return recurrences;}

    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    public void setStartTime(String startTime) { this.startTime = startTime;}
    public void setEndTime(String endTime) { this.endTime = endTime;}
    public void setRecurrences(List<RangerValidityRecurrence> recurrences) { this.recurrences = recurrences == null ? new ArrayList<>() : recurrences;}

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RangerValiditySchedule={");

        sb.append(", timeZone=").append(timeZone);
        sb.append(", startTime=").append(startTime);
        sb.append(", endTime=").append(endTime);

        sb.append(", recurrences=").append(Arrays.toString(getRecurrences().toArray()));

        return sb.toString();
    }
}
