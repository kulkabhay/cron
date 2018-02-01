package test;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

        LOG.info("Incoming zone in milliseconds:[" + ret + "]");
        if (StringUtils.isNotBlank(zone.timeZone)) {
            TimeZone defaultTZ = TimeZone.getDefault();
            TimeZone zoneTZ = TimeZone.getTimeZone(zone.timeZone);
            LOG.info("defaultTZ:[" + defaultTZ +"], zoneTZ:[" + zoneTZ +"]");
            if (!defaultTZ.equals(zoneTZ)) {
                int offset = defaultTZ.getOffset(ret);

                LOG.info("Offset:[" + offset +"]");

                ret += offset;
            }
        }

        LOG.info("Returning zone in milliseconds:[" + ret + "]");

        return ret;


    }

}
