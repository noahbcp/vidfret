package vidfret;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for extracting and manipulating ImageJ2 Datasets.
 * Handles conversion between ImageJ2's n-dimensional Dataset and simple 2D float arrays.
 */
public class DatasetUtils {

    /**
     * Extract a 2D float plane from a Dataset at given channel, timepoint, and z-slice.
     * 
     * @param dataset Multi-dimensional Dataset
     * @param channel Channel index (1-based, as used by ImageJ)
     * @param timeIndex Time index (0-based)
     * @param zIndex Z-slice index (0-based)
     * @return 2D float array [y][x]
     */
    public static float[][] extractPlane(Dataset dataset, int channel, long timeIndex, int zIndex) {
        int width = (int) dataset.dimension(Axes.X);
        int height = (int) dataset.dimension(Axes.Y);
        float[][] plane = new float[height][width];

        // Get the RandomAccessibleInterval for this dataset
        RandomAccessibleInterval<?> data = dataset.getImgPlus();

        // Create a view that slices to the specific channel, time, and z
        RandomAccessibleInterval<?> sliced = sliceData(data, dataset, channel, timeIndex, zIndex);

        // Copy values into float array
        copyToFloatArray(sliced, plane);

        return plane;
    }

    /**
     * Slice the n-dimensional data down to 2D (XY) at specific channel, time, and z indices.
     */
    private static RandomAccessibleInterval<?> sliceData(RandomAccessibleInterval<?> data, Dataset dataset,
                                                         int channel, long timeIndex, int zIndex) {
        RandomAccessibleInterval<?> result = data;

        // Collect slices to perform, with their dimension indices
        List<SliceOperation> slices = new ArrayList<>();

        int channelIdx = dataset.dimensionIndex(Axes.CHANNEL);
        if (channelIdx >= 0) {
            slices.add(new SliceOperation(channelIdx, (long) channel - 1));
        }

        int timeIdx = dataset.dimensionIndex(Axes.TIME);
        if (timeIdx >= 0) {
            slices.add(new SliceOperation(timeIdx, timeIndex));
        }

        int zIdx = dataset.dimensionIndex(Axes.Z);
        if (zIdx >= 0) {
            slices.add(new SliceOperation(zIdx, (long) zIndex));
        }

        // Sort slices by dimension index in descending order (highest first)
        slices.sort((a, b) -> Integer.compare(b.dimensionIndex, a.dimensionIndex));

        // Apply slices in order
        for (SliceOperation slice : slices) {
            result = Views.hyperSlice(result, slice.dimensionIndex, slice.sliceValue);
        }

        return result;
    }

    private static class SliceOperation {
        final int dimensionIndex;
        final long sliceValue;

        SliceOperation(int dimensionIndex, long sliceValue) {
            this.dimensionIndex = dimensionIndex;
            this.sliceValue = sliceValue;
        }
    }

    /**
     * Copy 2D RandomAccessibleInterval values into a float array.
     */
    private static void copyToFloatArray(RandomAccessibleInterval<?> interval, float[][] array) {
        int width = array[0].length;

        Cursor<?> cursor = Views.flatIterable(interval).cursor();
        int y = 0;
        int x = 0;

        while (cursor.hasNext()) {
            Object val = cursor.next();
            if (val instanceof RealType) {
                array[y][x] = ((RealType<?>) val).getRealFloat();
            }
            
            x++;
            if (x >= width) {
                x = 0;
                y++;
            }
        }
    }

    /**
     * Get the number of timepoints in the dataset.
     */
    public static long getTimePointCount(Dataset dataset) {
        int timeIdx = dataset.dimensionIndex(Axes.TIME);
        if (timeIdx >= 0) {
            return dataset.dimension(Axes.TIME);
        }
        return 1;
    }

    /**
     * Get the number of Z-slices in the dataset.
     */
    public static int getZSliceCount(Dataset dataset) {
        int zIdx = dataset.dimensionIndex(Axes.Z);
        if (zIdx >= 0) {
            return (int) dataset.dimension(Axes.Z);
        }
        return 1;
    }

    /**
     * Check if dataset has specified channel.
     */
    public static boolean hasChannel(Dataset dataset, int channel) {
        int numChannels = (int) dataset.dimension(Axes.CHANNEL);
        return channel > 0 && channel <= numChannels;
    }
}
