package vidfret;

import net.imagej.Dataset;
import net.imagej.axis.Axes;

/**
 * High-level FRET analysis orchestrator.
 * Coordinates the workflow for analyzing single frames, Z-stacks, and time-series.
 * Entry point for programmatic FRET analysis.
 */
public class FretAnalysisService {
    
    private final FretCalculationService calculationService;
    private final ImageProcessingService imageProcessingService;

    public FretAnalysisService() {
        this.calculationService = new FretCalculationService();
        this.imageProcessingService = new ImageProcessingService();
    }

    /**
     * Analyze a single 2D frame.
     * 
     * @param dataset Multi-dimensional dataset
     * @param params FRET parameters (includes channel selection, background, etc.)
     * @return Analysis result for single frame
     * @throws IllegalArgumentException if dataset doesn't have required channels
     */
    public FretAnalysisResult analyzeFrame(Dataset dataset, FretParams params) {
        // TODO: Extract 2D planes from dataset using ImageJ2 API
        // For now, placeholder
        throw new UnsupportedOperationException("ImageJ2 Dataset integration needed");
    }

    /**
     * Analyze complete Z-stack.
     * 
     * @param dataset Multi-dimensional dataset with Z dimension
     * @param zRange Which Z slices to analyze
     * @param params FRET parameters
     * @return Array of results, one per Z-slice
     */
    public FretAnalysisResult[] analyzeZStack(Dataset dataset, ZRange zRange, FretParams params) {
        // TODO: Iterate through Z dimension, extract planes, analyze each
        throw new UnsupportedOperationException("ImageJ2 Dataset integration needed");
    }

    /**
     * Analyze time-series of frames.
     * 
     * @param dataset Multi-dimensional dataset with TIME dimension
     * @param timeRange Which frames to analyze
     * @param params FRET parameters
     * @return Array of results, one per timepoint
     */
    public FretAnalysisResult[] analyzeTimeSeries(Dataset dataset, TimeRange timeRange, FretParams params) {
        // TODO: Iterate through TIME dimension, extract planes, analyze each
        throw new UnsupportedOperationException("ImageJ2 Dataset integration needed");
    }

    /**
     * Analyze complete 4D dataset (time x Z x Y x X).
     * 
     * @param dataset 4D dataset
     * @param timeRange Frame range to analyze
     * @param zRange Z-slice range to analyze
     * @param params FRET parameters
     * @return 2D array of results [timepoint][zslice]
     */
    public FretAnalysisResult[][] analyzeTimeSeries4D(Dataset dataset, TimeRange timeRange, 
                                                      ZRange zRange, FretParams params) {
        // TODO: Full 4D iteration
        throw new UnsupportedOperationException("ImageJ2 Dataset integration needed");
    }

    /**
     * Analyze pre-extracted 2D planes directly (useful for batch processing).
     * 
     * @param fretPlane 2D array for FRET channel
     * @param donorPlane 2D array for Donor channel
     * @param acceptorPlane 2D array for Acceptor channel
     * @param params FRET parameters
     * @return Analysis result
     */
    public FretAnalysisResult analyzePlanes(float[][] fretPlane, float[][] donorPlane,
                                           float[][] acceptorPlane, FretParams params) {
        // Apply background subtraction
        float bg_fret = params.getBackground().getFretBackground();
        float bg_donor = params.getBackground().getDonorBackground();
        float bg_acceptor = params.getBackground().getAcceptorBackground();
        
        float[][] fret_sub = imageProcessingService.subtractBackground(fretPlane, bg_fret);
        float[][] donor_sub = imageProcessingService.subtractBackground(donorPlane, bg_donor);
        float[][] acceptor_sub = imageProcessingService.subtractBackground(acceptorPlane, bg_acceptor);
        
        // Apply smoothing if needed
        float sigma = params.getGaussianSigma();
        if (sigma > 0) {
            fret_sub = imageProcessingService.smoothGaussian(fret_sub, sigma);
            donor_sub = imageProcessingService.smoothGaussian(donor_sub, sigma);
            acceptor_sub = imageProcessingService.smoothGaussian(acceptor_sub, sigma);
        }
        
        // Calculate FRET
        return calculationService.analyzePlane(fret_sub, donor_sub, acceptor_sub, params);
    }

    /**
     * Validate that dataset has required structure for FRET analysis.
     * 
     * @param dataset Input dataset
     * @param params Parameter object specifying required channels
     * @return true if dataset is compatible
     */
    public boolean validateDataset(Dataset dataset, FretParams params) {
        int numChannels = (int) dataset.dimension(Axes.CHANNEL);
        int maxChannel = Math.max(params.getDonorChannel(), 
                                  Math.max(params.getFretChannel(), params.getAcceptorChannel()));
        
        return numChannels >= maxChannel;
    }
}
