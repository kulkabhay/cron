package test;

public class ScheduledTimeRangeMatcher implements ScheduledTimeMatcher {
    private int lowerBound;
    private int upperBound;

    ScheduledTimeRangeMatcher(int lowerBound, int upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }
    @Override
    public boolean isMatch(int currentTime) {
        return currentTime == lowerBound || currentTime == upperBound || (currentTime > lowerBound && currentTime < upperBound);
    }
}
