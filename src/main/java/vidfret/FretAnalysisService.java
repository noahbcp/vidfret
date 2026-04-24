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
