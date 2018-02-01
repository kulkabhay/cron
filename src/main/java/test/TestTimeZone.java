package test;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

public class TestTimeZone {

    private static final Log LOG = LogFactory.getLog(TestTimeZone.class);

    private static TimeZone defaultTZ = TimeZone.getDefault();

    private Date startTime;
    private String timeZone;

    public TestTimeZone() {
    }

    public TestTimeZone(Date startTime, String timeZone) {
        setStartTime(startTime);
        setTimeZone(timeZone);
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Date getStartTime() {
        return startTime;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public String toString() {
        return "startTime:[" + startTime + "], timeZone:[" + timeZone + "]";
    }

    static Date getAdjustedTime(Date date, String timeZoneId) {
        Date ret = date;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Input:[" + date + ", " + timeZoneId + "]");
        }
        //LOG.info("List of time-zones:[" + Arrays.asList(TimeZone.getAvailableIDs()) +"]");
        if (StringUtils.isNotBlank(timeZoneId)) {
            TimeZone targetTZ = TimeZone.getTimeZone(timeZoneId);
            if (LOG.isDebugEnabled()) {
                LOG.debug("defaultTZ:[" + defaultTZ + "], zoneTZ:[" + targetTZ + "]");
            }
            if (!defaultTZ.equals(targetTZ)) {
                long timeInMs = ret.getTime();
                int offsetFromTarget = targetTZ.getOffset(timeInMs);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Offset of targetTZ :[" + offsetFromTarget + "]");
                }

                int offsetFromDefault = defaultTZ.getOffset(timeInMs);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Offset of defaultTZ :[" + offsetFromDefault + "]");
                }

                timeInMs += (offsetFromTarget - offsetFromDefault);
                ret = new Date(timeInMs);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Output:[" + ret + "]");
        }

        return ret;


    }

}
