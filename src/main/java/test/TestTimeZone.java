package test;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class TestTimeZone {

    private static final Log LOG = LogFactory.getLog(TestTimeZone.class);

    private Date startTime;
    private String timeZone;

    public TestTimeZone() {}

    public TestTimeZone(Date startTime, String timeZone) {
        setStartTime(startTime);
        setTimeZone(timeZone);
    }
    public void setStartTime(Date startTime) { this.startTime = startTime;}
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

    public Date getStartTime() { return startTime;}
    public String getTimeZone() { return timeZone; }

    public String toString() {
        return "startTime:[" + startTime +"], timeZone:[" + "]";
    }

    static long getStartTimeInMillis(TestTimeZone zone) {
        long ret = zone.startTime.getTime();

        LOG.info("Incoming startTime:[" + zone.startTime + "]");
        LOG.info("List of time-zones:[" + Arrays.asList(TimeZone.getAvailableIDs()) +"]");
        if (StringUtils.isNotBlank(zone.timeZone)) {
            TimeZone defaultTZ = TimeZone.getDefault();
            TimeZone zoneTZ = TimeZone.getTimeZone(zone.timeZone);
            TimeZone UTC = TimeZone.getTimeZone("UTC");
            LOG.info("defaultTZ:[" + defaultTZ +"], zoneTZ:[" + zoneTZ +"]");
            LOG.info("UTC:[" + UTC +"]");
            if (!defaultTZ.equals(zoneTZ)) {
                int offset = zoneTZ.getOffset(ret);

                LOG.info("Offset of zoneTZ :[" + offset +"]");

                int fromDefault = defaultTZ.getOffset(ret);
                LOG.info("Offset of defaultTZ :[" + fromDefault +"]");

                ret += offset;
                ret -= fromDefault;
            }
        }

        LOG.info("Returning modified startTime :[" + (new Date(ret)) + "]");

        return ret;


    }

}
