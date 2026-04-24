package vidfret;

/**
 * Configuration object encapsulating all FRET analysis parameters.
 * This is immutable after construction for thread-safety and clarity.
 */
public class FretParams {
    // Channel indices (1-based for ImageJ compatibility)
    private final int donorChannel;
    private final int fretChannel;
    private final int acceptorChannel;

    // Bleed-through calibration (pre-computed from reference samples)
    // Format: [modelType][coefficient] where modelType is CST/LIN/EXP (0/1/2)
    private final float[][] donorBleedThrough;
    private final float[][] acceptorBleedThrough;
    private final int donorModel;
    private final int acceptorModel;

    // Background subtraction values
    private final FretBackground background;

    // Processing options
    private final float gaussianSigma;
    private final int normalisationMethod; // 0=FRET/Donor, 1=FRET/Acceptor, 2=FRET/(D*A), 3=FRET/sqrt(D*A), 4=FRET Efficiency
    private final float thresholdFactor;

    private FretParams(Builder builder) {
        this.donorChannel = builder.donorChannel;
        this.fretChannel = builder.fretChannel;
        this.acceptorChannel = builder.acceptorChannel;
        this.donorBleedThrough = builder.donorBleedThrough;
        this.acceptorBleedThrough = builder.acceptorBleedThrough;
        this.donorModel = builder.donorModel;
        this.acceptorModel = builder.acceptorModel;
        this.background = builder.background;
        this.gaussianSigma = builder.gaussianSigma;
        this.normalisationMethod = builder.normalisationMethod;
        this.thresholdFactor = builder.thresholdFactor;
    }

    // Getters
    public int getDonorChannel() { return donorChannel; }
    public int getFretChannel() { return fretChannel; }
    public int getAcceptorChannel() { return acceptorChannel; }
    public float[][] getDonorBleedThrough() { return donorBleedThrough; }
    public float[][] getAcceptorBleedThrough() { return acceptorBleedThrough; }
    public int getDonorModel() { return donorModel; }
    public int getAcceptorModel() { return acceptorModel; }
    public FretBackground getBackground() { return background; }
    public float getGaussianSigma() { return gaussianSigma; }
    public int getnormalisationMethod() { return normalisationMethod; }
    public float getThresholdFactor() { return thresholdFactor; }

    // Builder pattern for easier construction
    public static class Builder {
        private int donorChannel = 1;
        private int fretChannel = 2;
        private int acceptorChannel = 3;
        private float[][] donorBleedThrough = new float[3][3];
        private float[][] acceptorBleedThrough = new float[3][3];
        private int donorModel = 0;
        private int acceptorModel = 0;
        private FretBackground background = new FretBackground(0, 0, 0);
        private float gaussianSigma = 0;
        private int normalisationMethod = 0;
        private float thresholdFactor = 1.0f;

        public Builder donorChannel(int ch) { this.donorChannel = ch; return this; }
        public Builder fretChannel(int ch) { this.fretChannel = ch; return this; }
        public Builder acceptorChannel(int ch) { this.acceptorChannel = ch; return this; }
        public Builder donorBleedThrough(float[][] bt) { this.donorBleedThrough = bt; return this; }
        public Builder acceptorBleedThrough(float[][] bt) { this.acceptorBleedThrough = bt; return this; }
        public Builder donorModel(int model) { this.donorModel = model; return this; }
        public Builder acceptorModel(int model) { this.acceptorModel = model; return this; }
        public Builder background(FretBackground bg) { this.background = bg; return this; }
        public Builder gaussianSigma(float sigma) { this.gaussianSigma = sigma; return this; }
        public Builder normalisationMethod(int method) { this.normalisationMethod = method; return this; }
        public Builder thresholdFactor(float factor) { this.thresholdFactor = factor; return this; }

        public FretParams build() {
            return new FretParams(this);
        }
    }
}