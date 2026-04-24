package vidfret;

/**
 * Encapsulates background values for FRET, Donor, and Acceptor channels.
 */
public class FretBackground {
    private final float fretBackground;
    private final float donorBackground;
    private final float acceptorBackground;

    public FretBackground(float fretBg, float donorBg, float acceptorBg) {
        this.fretBackground = fretBg;
        this.donorBackground = donorBg;
        this.acceptorBackground = acceptorBg;
    }

    public float getFretBackground() { return fretBackground; }
    public float getDonorBackground() { return donorBackground; }
    public float getAcceptorBackground() { return acceptorBackground; }
}
