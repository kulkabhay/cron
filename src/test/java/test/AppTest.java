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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ranger.plugin.model.validation.ValidationFailureDetails;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Unit tests for chron.
 */
public class AppTest 
    extends TestCase
{
    private static final Log LOG = LogFactory.getLog(AppTest.class);

    class TestResult {
        boolean isValid;
        int validationFailureCount;
        boolean isApplicable;
    }
    class TestCase {
        String name;
        List<RangerValiditySchedule> validitySchedules;
        Date accessTime;
        TestResult result;
    }

    private static Gson gson;

    static {
        GsonBuilder builder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSSZ");
        gson = builder
                .setPrettyPrinting()
                .create();
    }

    private List<TestCase> getTestCases(String fileName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> init()" );
        }

        List<TestCase> ret = null;
        Reader reader = null;
        URL testCasesURL = null;

        try {
            testCasesURL = getInputFileURL(fileName);
            InputStream in = testCasesURL.openStream();
            reader = new InputStreamReader(in, Charset.forName("UTF-8"));
            Type listType = new TypeToken<List<TestCase>>() {
            }.getType();
            ret = gson.fromJson(reader, listType);
        }
        catch (Exception excp) {
            LOG.error("Error opening request data stream or loading load request data from file, URL=" + testCasesURL, excp);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception excp) {
                    LOG.error("Error closing file ", excp);
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== init() : " + ret );
        }
        return ret;
    }

    private static URL getInputFileURL(final String name) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getInputFileURL(" + name + ")");
        }
        URL ret = null;
        InputStream in = null;


        if (StringUtils.isNotBlank(name)) {

            File f = new File(name);

            if (f.exists() && f.isFile() && f.canRead()) {
                try {

                    in = new FileInputStream(f);
                    ret = f.toURI().toURL();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("URL:" + ret);
                    }

                } catch (FileNotFoundException exception) {
                    LOG.error("Error processing input file:" + name + " or no privilege for reading file " + name, exception);
                } catch (MalformedURLException malformedException) {
                    LOG.error("Error processing input file:" + name + " cannot be converted to URL " + name, malformedException);
                }
            } else {

                URL fileURL = App.class.getResource(name);
                if (fileURL == null) {
                    if (!name.startsWith("/")) {
                        fileURL = App.class.getResource("/" + name);
                    }
                }

                if (fileURL == null) {
                    fileURL = ClassLoader.getSystemClassLoader().getResource(name);
                    if (fileURL == null) {
                        if (!name.startsWith("/")) {
                            fileURL = ClassLoader.getSystemClassLoader().getResource("/" + name);
                        }
                    }
                }

                if (fileURL != null) {
                    try {
                        in = fileURL.openStream();
                        ret = fileURL;
                    } catch (Exception exception) {
                        LOG.error(name + " cannot be opened:", exception);
                    }
                } else {
                    LOG.warn("Error processing input file: URL not found for " + name + " or no privilege for reading file " + name);
                }
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getInputFileURL(" + name + ", URL=" + ret + ")");
        }
        return ret;
    }
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    public void testTimeZone() {
        List<TestTimeZone> ret = null;
        Reader reader = null;
        URL testCasesURL = null;

        try {
            testCasesURL = getInputFileURL("/test-time-zone.json");
            InputStream in = testCasesURL.openStream();
            reader = new InputStreamReader(in, Charset.forName("UTF-8"));
            Type listType = new TypeToken<List<TestTimeZone>>() {
            }.getType();
            ret = gson.fromJson(reader, listType);
        }
        catch (Exception excp) {
            LOG.error("Error opening request data stream or loading load request data from file, URL=" + testCasesURL, excp);
        }
        for (TestTimeZone timeSpec : ret) {
            TestTimeZone.getAdjustedTime(timeSpec.getStartTime(), timeSpec.getTimeZone());
        }
    }

    public void testRangerValiditySchedulesForFailuresNew() {
        readAndRunTests("/validity-schedules-invalid-new.json");
    }
    public void testRangerValiditySchedulesForFailures() {
        readAndRunTests("/validity-schedules-invalid.json");
    }
    public void testRangerValiditySchedulesForValidity() {
        readAndRunTests("/validity-schedules-valid.json");
    }
    public void testRangerValiditySchedulesForApplicability() {
        readAndRunTests("/validity-schedules-valid-and-applicable.json");
    }

    private void readAndRunTests(String testFileName)
    {
        List<TestCase> testCases = getTestCases(testFileName);

        if (CollectionUtils.isNotEmpty(testCases)) {
            for (TestCase testCase : testCases) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Running testCase:[" + testCase.name + "]");
                }
                boolean isValid = true;
                List<ValidationFailureDetails> validationFailures = new ArrayList<>();
                boolean isApplicable = false;

                List<RangerValiditySchedule> validatedSchedules = new ArrayList<>();

                for (RangerValiditySchedule validitySchedule : testCase.validitySchedules) {
                    RangerValidityScheduleValidator validator = new RangerValidityScheduleValidator(validitySchedule);
                    RangerValiditySchedule validatedSchedule = validator.validate(validationFailures);
                    isValid = isValid && validatedSchedule != null;
                    if (isValid) {
                        validatedSchedules.add(validatedSchedule);
                    }
                }
                if (isValid) {
                    for (RangerValiditySchedule validSchedule : validatedSchedules) {
                        isApplicable = new RangerValidityScheduleEvaluator(validSchedule).isApplicable(testCase.accessTime.getTime());
                        if (isApplicable) {
                            break;
                        }
                    }
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("validationFailureDetails:" + validationFailures);
                }

                assertTrue(isValid == testCase.result.isValid);
                assertTrue(isApplicable == testCase.result.isApplicable);
                assertTrue(validationFailures.size() == testCase.result.validationFailureCount);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Completed testCase:[" + testCase.name + "]");
                }
            }
        }
    }
}
