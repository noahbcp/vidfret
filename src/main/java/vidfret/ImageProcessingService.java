package vidfret;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Image processing operations for FRET analysis.
 * Handles smoothing, background subtraction, and other image operations.
 * Uses ImgLib2 for n-dimensional image support.
 */
public class ImageProcessingService {

    /**
     * Apply Gaussian blur to a 2D image plane.
     * 
     * @param plane 2D float array
     * @param sigma Gaussian sigma value
     * @return Smoothed 2D array
     */
    public float[][] smoothGaussian(float[][] plane, float sigma) {
        if (sigma <= 0) return plane;
        
        int height = plane.length;
        int width = plane[0].length;
        
        // Flatten 2D array to 1D for ImgLib2
        float[] flat = new float[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                flat[y * width + x] = plane[y][x];
            }
        }
        
        // Create ImgLib2 image
        RandomAccessibleInterval<FloatType> img = ArrayImgs.floats(flat, width, height);
        
        // Extend with zero boundary
        var extended = Views.extendZero(img);
        
        // Apply Gaussian blur
        Gauss3.gauss(new double[]{sigma, sigma}, extended, img);
        
        // Copy back to 2D array
        float[][] smoothed = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                smoothed[y][x] = img.getAt(x, y).get();
            }
        }
        
        return smoothed;
    }

    /**
     * Subtract background from image.
     * 
     * @param plane Input image
     * @param background Background value to subtract
     * @return Image with background subtracted
     */
    public float[][] subtractBackground(float[][] plane, float background) {
        int height = plane.length;
        int width = plane[0].length;
        float[][] result = new float[height][width];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = plane[y][x] - background;
            }
        }
        
        return result;
    }

    /**
     * Get statistics of image plane.
     * 
     * @param plane 2D image array
     * @return [min, max, mean, median]
     */
    public float[] getStatistics(float[][] plane) {
        int height = plane.length;
        int width = plane[0].length;
        
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        float sum = 0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float val = plane[y][x];
                min = Math.min(min, val);
                max = Math.max(max, val);
                sum += val;
            }
        }
        
        float mean = sum / (height * width);

        return new float[]{min, max, mean};
    }

    /**
     * Compute Otsu's auto-threshold for a 2D image plane.
     * This is the "Default" method in ImageJ.
     *
     * @param plane 2D image array
     * @return Threshold value
     */
    public float computeOtsuThreshold(float[][] plane) {
        int height = plane.length;
        int width = plane[0].length;
        int totalPixels = height * width;

        // Compute histogram (using 256 bins)
        int[] histogram = new int[256];
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        // Find min/max to scale to 0-255
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float val = plane[y][x];
                min = Math.min(min, val);
                max = Math.max(max, val);
            }
        }

        if (max == min) return min; // Uniform image

        // Fill histogram
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float val = plane[y][x];
                int bin = (int) ((val - min) / (max - min) * 255);
                bin = Math.max(0, Math.min(255, bin));
                histogram[bin]++;
            }
        }

        // Otsu's method
        float sum = 0;
        for (int i = 0; i < 256; i++) sum += i * histogram[i];

        float sumB = 0;
        float wB = 0;
        float wF = 0;
        float maxVariance = 0;
        float threshold = 0;

        for (int t = 0; t < 256; t++) {
            wB += histogram[t];
            if (wB == 0) continue;

            wF = totalPixels - wB;
            if (wF == 0) break;

            sumB += (float) (t * histogram[t]);

            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;

            float between = wB * wF * (mB - mF) * (mB - mF);

            if (between > maxVariance) {
                maxVariance = between;
                threshold = t;
            }
        }

        // Convert threshold back to original scale
        return min + (threshold / 255.0f) * (max - min);
    }

    /**
     * Compute auto-threshold for a 2D image plane using specified method.
     * Methods: "Default (Otsu)", "Mean", "Minimum", "Triangle"
     * 
     * @param plane 2D image array
     * @param method Threshold method name
     * @return Threshold value
     */
    public float computeAutoThreshold(float[][] plane, String method) {
        if (method == null) method = "Default (Otsu)";
        
        String methodLower = method.toLowerCase();
        
        // Handle "Default (Otsu)" or "Otsu" or "Default"
        if (methodLower.contains("otsu") || methodLower.contains("default")) {
            return computeOtsuThreshold(plane);
        }
        
        switch(methodLower) {
            case "mean":
                return computeMeanThreshold(plane);
            case "minimum":
                return computeMinimumThreshold(plane);
            case "triangle":
                return computeTriangleThreshold(plane);
            default:
                return computeOtsuThreshold(plane); // Fallback to Otsu
        }
    }

    /**
     * Compute Mean threshold (mean of image).
     */
    private float computeMeanThreshold(float[][] plane) {
        int height = plane.length;
        int width = plane[0].length;
        float sum = 0;
        int count = 0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sum += plane[y][x];
                count++;
            }
        }
        return sum / count;
    }

    /**
     * Compute Minimum threshold (simplified).
     */
    private float computeMinimumThreshold(float[][] plane) {
        float[] stats = getStatistics(plane);
        return (stats[0] + stats[1]) / 2; // (min + max) / 2
    }

    /**
     * Compute Triangle threshold (simplified).
     */
    private float computeTriangleThreshold(float[][] plane) {
        float[] stats = getStatistics(plane);
        return stats[1] * 0.5f; // Rough approximation: half of max
    }

    /**
     * Create a binary mask from a plane using auto-threshold.
     * Pixels above threshold are set to 1, below to 0.
     * 
     * @param plane 2D image array
     * @param method Threshold method ("Default", "Mean", etc.)
     * @return Binary mask (1 for signal, 0 for background)
     */
    public float[][] createAutoThresholdMask(float[][] plane, String method) {
        float threshold = computeAutoThreshold(plane, method);
        int height = plane.length;
        int width = plane[0].length;
        
        float[][] mask = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                mask[y][x] = (plane[y][x] > threshold) ? 1.0f : 0.0f;
            }
        }
        return mask;
    }

    /**
     * Apply a mask to an image plane (multiply).
     * Pixels where mask is 0 become 0.
     *
     * @param plane Image to mask
     * @param mask Binary mask (1 for keep, 0 for zero)
     * @return Masked image
     */
    public float[][] applyMask(float[][] plane, float[][] mask) {
        int height = plane.length;
        int width = plane[0].length;

        float[][] result = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = plane[y][x] * mask[y][x];
            }
        }
        return result;
    }
}
