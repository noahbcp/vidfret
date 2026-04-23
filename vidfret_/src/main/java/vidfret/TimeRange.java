package vidfret;

/**
 * Specifies a time range for analysis.
 */
public class TimeRange {
    private final long startFrame;
    private final long endFrame;

    public TimeRange(long startFrame, long endFrame) {
        if (startFrame > endFrame) {
            throw new IllegalArgumentException("Start frame cannot be after end frame");
        }
        this.startFrame = startFrame;
        this.endFrame = endFrame;
    }

    public long getStartFrame() { return startFrame; }
    public long getEndFrame() { return endFrame; }
    public long getFrameCount() { return endFrame - startFrame + 1; }
}