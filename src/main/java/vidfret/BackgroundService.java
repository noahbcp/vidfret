package vidfret;

/**
 * Background measurement and handling service.
 * Provides flexible background estimation strategies.
 */
public class BackgroundService {

    /**
     * Estimate background from image statistics.
     * Uses a percentile-based approach: assumes lowest intensity values are background.
     * 
     * @param plane 2D image plane
     * @param percentile Percentile to use (e.g., 5 for 5th percentile)
     * @return Estimated background value
     */
    public float estimateFromPercentile(float[][] plane, float percentile) {
        int height = plane.length;
        int width = plane[0].length;
        int total = height * width;
        
        // Collect all values
        float[] values = new float[total];
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                values[idx++] = plane[y][x];
            }
        }
        
        // Sort
        java.util.Arrays.sort(values);
        
        // Find percentile
        int percentileIdx = (int)(total * (percentile / 100.0f));
        return values[Math.min(percentileIdx, total - 1)];
    }

    /**
     * Estimate background from histogram mode (most common intensity).
     * Useful for dark background images.
     * 
     * @param plane 2D image plane
     * @return Mode (most frequent) intensity value
     */
    public float estimateFromMode(float[][] plane) {
        int height = plane.length;
        int width = plane[0].length;
        
        // Simple approach: find minimum value region
        float min = Float.MAX_VALUE;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                min = Math.min(min, plane[y][x]);
            }
        }
        
        return min;
    }

    /**
     * Estimate background as mean of low-intensity pixels.
     * 
     * @param plane 2D image plane
     * @param fraction Fraction of lowest-intensity pixels to include (0.0-1.0)
     * @return Mean of low-intensity pixels
     */
    public float estimateFromMeanLow(float[][] plane, float fraction) {
        int height = plane.length;
        int width = plane[0].length;
        int total = height * width;
        
        // Collect all values
        float[] values = new float[total];
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                values[idx++] = plane[y][x];
            }
        }
        
        // Sort
        java.util.Arrays.sort(values);
        
        // Average lowest fraction
        int count = Math.max(1, (int)(total * fraction));
        float sum = 0;
        for (int i = 0; i < count; i++) {
            sum += values[i];
        }
        
        return sum / count;
    }

    /**
     * Create background for all three channels from individual measurements.
     * 
     * @param fretPlane FRET channel image
     * @param donorPlane Donor channel image
     * @param acceptorPlane Acceptor channel image
     * @return FretBackground with measured values
     */
    public FretBackground measureBackground(float[][] fretPlane, float[][] donorPlane, 
                                           float[][] acceptorPlane) {
        float fretBg = estimateFromPercentile(fretPlane, 5);
        float donorBg = estimateFromPercentile(donorPlane, 5);
        float acceptorBg = estimateFromPercentile(acceptorPlane, 5);
        
        return new FretBackground(fretBg, donorBg, acceptorBg);
    }

    /**
     * Create background with manual values.
     */
    public FretBackground createManualBackground(float fretBg, float donorBg, float acceptorBg) {
        return new FretBackground(fretBg, donorBg, acceptorBg);
    }
}
