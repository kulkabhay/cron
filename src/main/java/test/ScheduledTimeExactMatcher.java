package test;

public class ScheduledTimeExactMatcher implements ScheduledTimeMatcher {
    private int scheduledTime;

    ScheduledTimeExactMatcher(int scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
    @Override
    public boolean isMatch(int currentTime) {
        if (currentTime == scheduledTime) {
            return true;
        }
        return false;
    }
}