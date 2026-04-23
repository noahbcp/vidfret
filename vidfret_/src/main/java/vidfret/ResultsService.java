package vidfret;

import net.imagej.Dataset;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Results handling and export service.
 * Formats analysis results into ImageJ-compatible datasets or exportable formats.
 */
public class ResultsService {

    /**
     * Create an ImageJ Dataset from a single analysis result.
     * 
     * @param result FRET analysis result
     * @param name Display name for the dataset
     * @param includeCorrected If true, export corrected values; if false, normalized
     * @return ImageJ Dataset
     */
    public Dataset formatAsDataset(FretAnalysisResult result, String name, boolean includeCorrected) {
        // TODO: Proper ImageJ2 Dataset creation with metadata
        // This requires ImageJ services which aren't available as static methods
        throw new UnsupportedOperationException("Requires ImageJ context");
    }

    /**
     * Combine multiple time-series results into a time-series dataset.
     * 
     * @param results Array of results, one per timepoint
     * @param name Dataset name
     * @return Time-series Dataset [T][Y][X]
     */
    public Dataset formatAsTimeSeries(FretAnalysisResult[] results, String name) {
        // TODO: Create dataset with TIME axis
        throw new UnsupportedOperationException("Requires ImageJ context");
    }

    /**
     * Combine Z-stack results into a 3D dataset.
     * 
     * @param results Array of results, one per Z-slice
     * @param name Dataset name
     * @return 3D Dataset [Z][Y][X]
     */
    public Dataset formatAsZStack(FretAnalysisResult[] results, String name) {
        // TODO: Create dataset with Z/DEPTH axis
        throw new UnsupportedOperationException("Requires ImageJ context");
    }

    /**
     * Export single frame result to CSV.
     * 
     * @param result Analysis result
     * @param outputPath File path to write to
     * @param includeCorrected Include corrected FRET values
     * @param includeNormalized Include normalized FRET values
     * @throws IOException If write fails
     */
    public void exportToCSV(FretAnalysisResult result, String outputPath, 
                           boolean includeCorrected, boolean includeNormalized) throws IOException {
        int width = result.getWidth();
        int height = result.getHeight();
        
        try (FileWriter writer = new FileWriter(new File(outputPath))) {
            // Write header
            StringBuilder header = new StringBuilder("X,Y");
            if (includeCorrected) header.append(",FRET_corrected");
            if (includeNormalized) header.append(",FRET_normalized");
            writer.write(header.toString() + "\n");
            
            // Write data
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    StringBuilder line = new StringBuilder();
                    line.append(x).append(",").append(y);
                    
                    if (includeCorrected) {
                        line.append(",").append(result.getPixelValue(x, y));
                    }
                    if (includeNormalized) {
                        line.append(",").append(result.getNormalizedPixelValue(x, y));
                    }
                    
                    writer.write(line.toString() + "\n");
                }
            }
        }
    }

    /**
     * Export time-series results to CSV (one file per timepoint, or combined).
     * 
     * @param results Array of results
     * @param outputDirectory Directory to write files
     * @param singleFile If true, write all timepoints to one file; if false, separate files
     * @throws IOException If write fails
     */
    public void exportTimeSeriesToCSV(FretAnalysisResult[] results, String outputDirectory, 
                                     boolean singleFile) throws IOException {
        if (singleFile) {
            exportTimeSeriesToSingleCSV(results, outputDirectory);
        } else {
            exportTimeSeriesMultipleCSV(results, outputDirectory);
        }
    }

    private void exportTimeSeriesToSingleCSV(FretAnalysisResult[] results, String outputDir) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();
        
        File file = new File(dir, "fret_timeseries.csv");
        try (FileWriter writer = new FileWriter(file)) {
            // Header
            writer.write("T,X,Y,FRET_corrected,FRET_normalized\n");
            
            // Data from all timepoints
            for (int t = 0; t < results.length; t++) {
                FretAnalysisResult result = results[t];
                int width = result.getWidth();
                int height = result.getHeight();
                
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        writer.write(String.format("%d,%d,%d,%f,%f\n",
                            t, x, y,
                            result.getPixelValue(x, y),
                            result.getNormalizedPixelValue(x, y)));
                    }
                }
            }
        }
    }

    private void exportTimeSeriesMultipleCSV(FretAnalysisResult[] results, String outputDir) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();
        
        for (int t = 0; t < results.length; t++) {
            File file = new File(dir, String.format("fret_t%03d.csv", t));
            try (FileWriter writer = new FileWriter(file)) {
                FretAnalysisResult result = results[t];
                
                // Header
                writer.write("X,Y,FRET_corrected,FRET_normalized\n");
                
                int width = result.getWidth();
                int height = result.getHeight();
                
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        writer.write(String.format("%d,%d,%f,%f\n",
                            x, y,
                            result.getPixelValue(x, y),
                            result.getNormalizedPixelValue(x, y)));
                    }
                }
            }
        }
    }

    /**
     * Generate summary statistics for a result.
     * 
     * @param result Analysis result
     * @return String with summary statistics
     */
    public String generateSummary(FretAnalysisResult result) {
        float[][] corrected = result.getFretValues();
        float[][] normalized = result.getNormalizedFret();
        
        float min_corr = Float.MAX_VALUE, max_corr = Float.MIN_VALUE;
        float min_norm = Float.MAX_VALUE, max_norm = Float.MIN_VALUE;
        float sum_corr = 0, sum_norm = 0;
        int count = 0;
        
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                float c = corrected[y][x];
                float n = normalized[y][x];
                
                if (c != 0) {
                    min_corr = Math.min(min_corr, c);
                    max_corr = Math.max(max_corr, c);
                    sum_corr += c;
                    count++;
                }
                if (n != 0) {
                    min_norm = Math.min(min_norm, n);
                    max_norm = Math.max(max_norm, n);
                    sum_norm += n;
                }
            }
        }
        
        float mean_corr = count > 0 ? sum_corr / count : 0;
        float mean_norm = count > 0 ? sum_norm / count : 0;
        
        return String.format(
            "Frame %d, Z=%d\n" +
            "Corrected FRET: min=%.2f, max=%.2f, mean=%.2f\n" +
            "Normalized FRET: min=%.2f, max=%.2f, mean=%.2f\n" +
            "Non-zero pixels: %d/%d",
            result.getFrameIndex(), result.getZSlice(),
            min_corr, max_corr, mean_corr,
            min_norm, max_norm, mean_norm,
            count, result.getWidth() * result.getHeight()
        );
    }
}
