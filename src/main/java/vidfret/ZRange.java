package vidfret;

/**
 * Specifies a Z-stack range for analysis.
 */
public class ZRange {
    private final int startSlice;
    private final int endSlice;

    public ZRange(int startSlice, int endSlice) {
        if (startSlice > endSlice) {
            throw new IllegalArgumentException("Start slice cannot be after end slice");
        }
        this.startSlice = startSlice;
        this.endSlice = endSlice;
    }

    public int getStartSlice() { return startSlice; }
    public int getEndSlice() { return endSlice; }
    public int getSliceCount() { return endSlice - startSlice + 1; }
}
