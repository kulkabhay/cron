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

        if (currentTime == lowerBound || currentTime == upperBound) {
            return true;
        }
        if (currentTime > lowerBound && currentTime < upperBound) {
            return true;
        }
        return false;
    }
}
