package vidfret;

/**
 * Result of FRET analysis for a single frame.
 * Contains computed FRET efficiency/expression values and metadata.
 */
public class FretAnalysisResult {
    private final float[][] fretValues;      // Raw FRET signal
    private final float[][] normalizedFret;  // Normalized FRET
    private final int width;
    private final int height;
    private final int frameIndex;
    private final int zSlice;

    public FretAnalysisResult(float[][] fret, float[][] normalized, int width, int height, 
                             int frameIndex, int zSlice) {
        this.fretValues = fret;
        this.normalizedFret = normalized;
        this.width = width;
        this.height = height;
        this.frameIndex = frameIndex;
        this.zSlice = zSlice;
    }

    public float[][] getFretValues() { return fretValues; }
    public float[][] getNormalizedFret() { return normalizedFret; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getFrameIndex() { return frameIndex; }
    public int getZSlice() { return zSlice; }

    public float getPixelValue(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return fretValues[y][x];
        }
        return Float.NaN;
    }

    public float getNormalizedPixelValue(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return normalizedFret[y][x];
        }
        return Float.NaN;
    }
}
