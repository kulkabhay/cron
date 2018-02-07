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
/*package org.apache.ranger.plugin.model;*/


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
