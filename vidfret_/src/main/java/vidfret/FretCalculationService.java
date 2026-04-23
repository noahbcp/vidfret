package vidfret;

/**
 * Core FRET calculation service.
 * Performs pixel-by-pixel FRET computation using bleed-through corrected model.
 * 
 * FRET corrected = FRET_raw - (bleed_donor + bleed_acceptor)
 * where bleed-through is pre-calibrated from reference samples.
 */
public class FretCalculationService {
    
    // Model types (matching PixFRET)
    private static final int CST = 0;  // Constant
    private static final int LIN = 1;  // Linear
    private static final int EXP = 2;  // Exponential

    /**
     * Calculate corrected FRET value for a single pixel.
     * 
     * @param fretPixel Raw FRET channel intensity
     * @param donorPixel Donor channel intensity
     * @param acceptorPixel Acceptor channel intensity
     * @param params FRET parameters including bleed-through calibration
     * @return Bleed-through corrected FRET value
     */
    public float calculateCorrectedFret(float fretPixel, float donorPixel, 
                                       float acceptorPixel, FretParams params) {
        float correctedFret = fretPixel;
        
        // Subtract donor bleed-through
        correctedFret -= getBleedThrough(donorPixel, params.getDonorModel(), 
                                         params.getDonorBleedThrough());
        
        // Subtract acceptor bleed-through
        correctedFret -= getBleedThrough(acceptorPixel, params.getAcceptorModel(), 
                                         params.getAcceptorBleedThrough());
        
        return correctedFret;
    }

    /**
     * Calculate normalized/efficiency FRET from corrected values.
     * 
     * @param correctedFret Bleed-through corrected FRET
     * @param donorPixel Donor intensity (background already subtracted)
     * @param acceptorPixel Acceptor intensity (background already subtracted)
     * @param normMethod Normalization method (0-4)
     * @return Normalized FRET value
     */
    public float normalize(float correctedFret, float donorPixel, 
                          float acceptorPixel, int normMethod) {
        float normTerm;
        
        switch(normMethod) {
            case 0: // FRET / Donor
                normTerm = Math.abs(donorPixel);
                break;
            case 1: // FRET / Acceptor
                normTerm = Math.abs(acceptorPixel);
                break;
            case 2: // FRET / (Donor * Acceptor)
                normTerm = Math.abs(donorPixel * acceptorPixel);
                break;
            case 3: // FRET / sqrt(Donor * Acceptor)
                normTerm = (float) Math.sqrt(Math.abs(donorPixel * acceptorPixel));
                break;
            case 4: // FRET Efficiency: 100 * FRET / (FRET + Donor)
                normTerm = Math.abs(donorPixel + correctedFret);
                if (normMethod == 4) {
                    // Special case for efficiency
                    return normTerm != 0 ? correctedFret * 100.0f / normTerm : 0;
                }
                break;
            default:
                normTerm = 1.0f;
        }
        
        return normTerm != 0 ? correctedFret * 100.0f / normTerm : 0;
    }

    /**
     * Calculate bleed-through for a channel based on model and parameters.
     * 
     * Models:
     * - Constant: BT = a
     * - Linear: BT = a + b*x
     * - Exponential: BT = b*exp(e*x)
     */
    private float getBleedThrough(float intensity, int modelType, float[][] params) {
        switch(modelType) {
            case CST:
                return params[0][0]; // Constant a
            case LIN:
                return params[0][0] + params[1][0] * intensity; // a + b*x
            case EXP:
                return params[0][0] + params[1][0] * (float)Math.exp(params[2][0] * intensity);
            default:
                return 0;
        }
    }

    /**
     * Analyze a 2D image plane with per-pixel FRET calculation.
     * 
     * @param fretPlane 2D array of FRET channel intensities
     * @param donorPlane 2D array of Donor channel intensities
     * @param acceptorPlane 2D array of Acceptor channel intensities
     * @param params FRET parameters
     * @return Result containing corrected and normalized FRET values
     */
    public FretAnalysisResult analyzePlane(float[][] fretPlane, float[][] donorPlane,
                                          float[][] acceptorPlane, FretParams params) {
        int height = fretPlane.length;
        int width = fretPlane[0].length;
        
        float[][] correctedFret = new float[height][width];
        float[][] normalizedFret = new float[height][width];
        
        FretBackground bg = params.getBackground();
        float bgFret = bg.getFretBackground();
        float bgDonor = bg.getDonorBackground();
        float bgAcceptor = bg.getAcceptorBackground();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float fret = fretPlane[y][x] - bgFret;
                float donor = donorPlane[y][x] - bgDonor;
                float acceptor = acceptorPlane[y][x] - bgAcceptor;
                
                // Only compute for pixels above threshold
                float avgDonor = (donor > bgDonor * params.getThresholdFactor()) ? donor : 0;
                float avgAcceptor = (acceptor > bgAcceptor * params.getThresholdFactor()) ? acceptor : 0;
                
                if (avgDonor > 0 && avgAcceptor > 0) {
                    float corrected = calculateCorrectedFret(fret, donor, acceptor, params);
                    float normalized = normalize(corrected, donor, acceptor, params.getNormalizationMethod());
                    
                    correctedFret[y][x] = corrected;
                    normalizedFret[y][x] = normalized;
                } else {
                    correctedFret[y][x] = 0;
                    normalizedFret[y][x] = 0;
                }
            }
        }
        
        return new FretAnalysisResult(correctedFret, normalizedFret, width, height, 0, 0);
    }
}