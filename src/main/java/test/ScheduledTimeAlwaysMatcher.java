package test;

public class ScheduledTimeAlwaysMatcher implements ScheduledTimeMatcher {
    @Override
    public boolean isMatch(int currentTime) {
        return true;
    }
}
