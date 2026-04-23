package vidfret;

import net.imagej.Dataset;
import net.imagej.axis.Axes;

public class FretInput {
    private Dataset dataset;
    private Long startFrame;
    private Long endFrame;

    public FretInput(Dataset dataset, Integer donorChannel, Integer fretChannel, Long startFrame, Long endFrame) {
        this.dataset = dataset;
        this.startFrame = startFrame;
        this.endFrame = endFrame;
    }

    public boolean checkChannel() {
        if (this.dataset.dimension(Axes.CHANNEL) < 2) {
            return false;
        } else {
            return true;
        }
    }

    public void coherceTime() throws Exception {
        if (this.dataset.dimensionIndex(Axes.TIME) != -1) {
            Long nFrames = this.dataset.dimension(Axes.TIME);
            // Check that input start / end frames are within range, coherce if not.
            if (this.endFrame < this.startFrame | this.startFrame > nFrames) {
                throw new Exception("Frame range incorrect.");
            }
            if (nFrames > this.endFrame) {
                this.endFrame = nFrames;
            }
        }
    }

    public FretInput validateInput() throws Exception {
        if (this.checkChannel() == false) {
            throw new Exception("Image has incorrect channel dimensions.");
        } else {
            coherceTime();
        }
        return this;
    }
}
