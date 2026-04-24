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
     * Extract 3x3 neighborhood around pixel.
     * Uses mirror border conditions.
     * 
     * @param plane 2D image array
     * @param x X coordinate
     * @param y Y coordinate
     * @return 3x3 array centered at (x,y)
     */
    public float[][] getNeighborhood(float[][] plane, int x, int y) {
        float[][] neighborhood = new float[3][3];
        int height = plane.length;
        int width = plane[0].length;
        
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = mirrorCoord(x + dx, width);
                int ny = mirrorCoord(y + dy, height);
                neighborhood[dy + 1][dx + 1] = plane[ny][nx];
            }
        }
        
        return neighborhood;
    }

    /**
     * Apply mirror boundary conditions for coordinate wrapping.
     */
    private int mirrorCoord(int coord, int max) {
        if (coord < 0) {
            return -coord - 1;
        } else if (coord >= max) {
            return 2 * max - coord - 1;
        }
        return coord;
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
     * Extract 2D plane from 3D image at given index.
     * 
     * @param image3d 3D array [z][y][x]
     * @param zIndex Z coordinate
     * @return 2D array [y][x]
     */
    public float[][] extractPlane(float[][][] image3d, int zIndex) {
        if (zIndex >= 0 && zIndex < image3d.length) {
            return image3d[zIndex];
        }
        throw new IllegalArgumentException("Z index out of bounds");
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
}
