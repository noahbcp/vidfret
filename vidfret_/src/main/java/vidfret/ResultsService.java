package vidfret;

/**
 * Results handling and export service.
 * Formats analysis results into ImageJ-compatible datasets or exportable formats.
 */
public class ResultsService {
    /**
     * Export single frame result to CSV.
     * 
     * @param result Analysis result
     * @param outputPath File path to write to
     * @param includeCorrected Include corrected FRET values
     * @param includeNormalized Include normalized FRET values
     * @throws IOException If write fails
     */
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
