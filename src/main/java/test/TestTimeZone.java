/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
