package test;

public class ScheduledTimeExactMatcher implements ScheduledTimeMatcher {
    private int scheduledTime;

    public ScheduledTimeExactMatcher(int scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
    @Override
    public boolean isMatch(int currentTime) {
        return currentTime == scheduledTime;
    }
}
