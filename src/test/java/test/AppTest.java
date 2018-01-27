package test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
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

    public void testReadValiditySchedules()
    {
        List<RangerValiditySchedule> validitySchedules = new App().getValiditySchedules("/validity-schedules.json");

        if (CollectionUtils.isNotEmpty(validitySchedules)) {
            System.out.println();
            for (RangerValiditySchedule entry : validitySchedules) {
                System.out.println(entry);
            }
            System.out.println();
        }
        assertTrue(CollectionUtils.isNotEmpty(validitySchedules));
    }

    public void testValidateValiditySchedule() {
        List<RangerValiditySchedule> validitySchedules = new App().getValiditySchedules("/validity-schedules-valid.json");
        if (CollectionUtils.isNotEmpty(validitySchedules)) {
            for (RangerValiditySchedule entry : validitySchedules) {
                System.out.println(entry);

                List<ValidationFailureDetails> validationFailures = new ArrayList<>();
                RangerValidityScheduleValidator validator = new RangerValidityScheduleValidator(entry);
                RangerValiditySchedule normalizedValiditySchedule = validator.validate(RangerValidityScheduleValidator.Action.CREATE, validationFailures);
                if (normalizedValiditySchedule == null) {
                    for (ValidationFailureDetails failure : validationFailures) {
                        System.out.println(failure);
                    }
                }
                System.out.println("Normalized-Ranger-Validity-Schedule=" + normalizedValiditySchedule);
                assert(normalizedValiditySchedule != null && validationFailures.isEmpty());
            }
        }
    }

    public void testValidateValidityScheduleForFailures() {
        List<RangerValiditySchedule> validitySchedules = new App().getValiditySchedules("/validity-schedules-invalid.json");
        if (CollectionUtils.isNotEmpty(validitySchedules)) {
            for (RangerValiditySchedule entry : validitySchedules) {
                System.out.println(entry);

                List<ValidationFailureDetails> validationFailures = new ArrayList<>();
                RangerValidityScheduleValidator validator = new RangerValidityScheduleValidator(entry);
                RangerValiditySchedule normalizedValiditySchedule = validator.validate(RangerValidityScheduleValidator.Action.CREATE, validationFailures);
                if (normalizedValiditySchedule == null) {
                    for (ValidationFailureDetails failure : validationFailures) {
                        System.out.println(failure);
                    }
                }
                System.out.println("Normalized-Ranger-Validity-Schedule=" + normalizedValiditySchedule);
                assert(normalizedValiditySchedule == null && !validationFailures.isEmpty());
            }
        }
    }

    public void testScheduleApplicability() {
        List<RangerValiditySchedule> validitySchedules = new App().getValiditySchedules("/validity-schedules-valid-and-applicable.json");
        boolean foundAnApplicableSchedule = false;
        for (RangerValiditySchedule entry : validitySchedules) {
            System.out.println(entry);
            List<ValidationFailureDetails> validationFailures = new ArrayList<>();
            RangerValidityScheduleValidator validator = new RangerValidityScheduleValidator(entry);

            RangerValiditySchedule normalizedValiditySchedule = validator.validate(RangerValidityScheduleValidator.Action.UPDATE, validationFailures);

            if (normalizedValiditySchedule != null) {
                boolean matched = new RangerValidityScheduleEvaluator(normalizedValiditySchedule).isApplicable(new Date().getTime());
                if (matched) {
                    System.out.println("Matched: Ranger-Validity-Schedule=" + normalizedValiditySchedule);
                    foundAnApplicableSchedule = true;
                } else {
                    System.out.println("Not Matched: Ranger-Validity-Schedule=" + normalizedValiditySchedule);
                }
            } else {
                System.out.println("Invalid: Ranger-Validity-Schedule=" + entry);
            }
        }
        assert(foundAnApplicableSchedule);
    }
}
