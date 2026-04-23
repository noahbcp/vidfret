import vidfret.*;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.img.array.ArrayImgs;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.UIService;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>vidFret")
public class vidFret_ implements Command {
    /* Parameters - UI Inputs */
    @Parameter
    private UIService uiService;

    @Parameter
    private LogService logService;

    @Parameter
    private DatasetService datasetService;

    // Processes currently active image
    @Parameter
    private Dataset dataset;

    // Channel configuration
    @Parameter(label = "<html>Donor channel (D<sub>D</sub>)</html>", min = "1", required = true)
    private Integer donorChannel = 1;

    @Parameter(label = "<html>FRET channel (D<sub>A</sub>)</html>", min = "1", required = true)
    private Integer fretChannel = 2;

    @Parameter(label = "<html>Acceptor channel (A)</html>", min = "1", required = false)
    private Integer acceptorChannel = 3;

    // Frame/Z range
    @Parameter(label = "Start frame", description = "Frame to start analysis from.", min = "1", required = false)
    private Long startFrame = (long) 1;

    @Parameter(label = "End frame", description = "Frame to end analysis at (use large number for all frames).", min = "1", required = false)
    private Long endFrame = Long.MAX_VALUE;

    // Processing options
    @Parameter(label = "Gaussian sigma (pixels)", description = "Smoothing kernel sigma (0 = no smoothing)", 
               min = "0", required = false)
    private Float gaussianSigma = 0.0f;

    @Parameter(label = "Background method", choices = {"Auto (5th percentile)", "Manual input"}, 
               required = false)
    private String backgroundMethod = "Auto (5th percentile)";

    @Parameter(label = "Donor background (if manual)", min = "0", required = false)
    private Float manualDonorBg = 0.0f;

    @Parameter(label = "FRET background (if manual)", min = "0", required = false)
    private Float manualFretBg = 0.0f;

    @Parameter(label = "Acceptor background (if manual)", min = "0", required = false)
    private Float manualAcceptorBg = 0.0f;

    @Parameter(label = "Normalization", 
               choices = {"FRET/Donor", "FRET/Acceptor", "FRET/(D*A)", "FRET/sqrt(D*A)", "FRET Efficiency"}, 
               required = false)
    private String normalizationChoice = "FRET/Donor";

    @Parameter(label = "Threshold factor", description = "Pixels below sqrt(Bg_D*Bg_A)*factor are masked",
               min = "0.1", max = "10.0", required = false)
    private Float thresholdFactor = 1.0f;

    // Main
    @Override
    public void run() {
        try {
            // Validate input dataset
            FretInput input = new FretInput(dataset, donorChannel, fretChannel, startFrame, endFrame);
            input.validateInput();
            
            logService.info("vidFRET Analysis Starting");
            logService.info("Dataset: " + dataset.getName());
            logService.info("Dimensions - X:" + dataset.dimension(Axes.X) + 
                           " Y:" + dataset.dimension(Axes.Y) + 
                           " C:" + dataset.dimension(Axes.CHANNEL));

            // Validate channels exist
            if (!DatasetUtils.hasChannel(dataset, donorChannel)) {
                throw new IllegalArgumentException("Donor channel " + donorChannel + " not found");
            }
            if (!DatasetUtils.hasChannel(dataset, fretChannel)) {
                throw new IllegalArgumentException("FRET channel " + fretChannel + " not found");
            }
            if (!DatasetUtils.hasChannel(dataset, acceptorChannel)) {
                throw new IllegalArgumentException("Acceptor channel " + acceptorChannel + " not found");
            }

            // Initialize services
            FretAnalysisService analysisService = new FretAnalysisService();
            BackgroundService bgService = new BackgroundService();
            ResultsService resultsService = new ResultsService();

            // Get time range
            long numTimepoints = DatasetUtils.getTimePointCount(dataset);
            long tStart = Math.max(0, startFrame - 1);
            long tEnd = Math.min(numTimepoints, endFrame);
            logService.info("Processing timepoints: " + (tStart + 1) + " to " + tEnd);

            // Get Z range
            int numZSlices = DatasetUtils.getZSliceCount(dataset);
            logService.info("Z-slices: " + numZSlices);

            // Build FretParams
            FretParams params = buildFretParams(bgService);
            logService.info("Background - FRET:" + params.getBackground().getFretBackground() +
                           " Donor:" + params.getBackground().getDonorBackground() +
                           " Acceptor:" + params.getBackground().getAcceptorBackground());

            // Process each frame and Z-slice
            logService.info("Starting analysis loop...");
            int totalFrames = (int) ((tEnd - tStart) * numZSlices);
            int processedFrames = 0;

            // Store results for output
            FretAnalysisResult[][][] results = new FretAnalysisResult[(int)(tEnd - tStart)][numZSlices][];
            
            for (long t = tStart; t < tEnd; t++) {
                for (int z = 0; z < numZSlices; z++) {
                    // Extract planes
                    float[][] fretPlane = DatasetUtils.extractPlane(dataset, fretChannel, t, z);
                    float[][] donorPlane = DatasetUtils.extractPlane(dataset, donorChannel, t, z);
                    float[][] acceptorPlane = DatasetUtils.extractPlane(dataset, acceptorChannel, t, z);

                    // Analyze
                    FretAnalysisResult result = analysisService.analyzePlanes(fretPlane, donorPlane, 
                                                                             acceptorPlane, params);

                    results[(int)(t - tStart)][z] = new FretAnalysisResult[]{result};
                    
                    processedFrames++;
                    int progress = (processedFrames * 100) / totalFrames;
                    logService.info("Progress: " + progress + "% (T:" + (t+1) + " Z:" + (z+1) + ")");
                }
            }

            logService.info("Analysis complete. Creating output datasets...");

            // Create and display result dataset
            createAndDisplayResults(results, params, resultsService);

            logService.info("vidFRET analysis finished successfully");

        } catch (Exception e) {
            logService.error("vidFRET analysis failed: " + e.getMessage());
            e.printStackTrace();
            uiService.showDialog(e.getMessage(), "Error", DialogPrompt.MessageType.ERROR_MESSAGE);
        }
    }

    /**
     * Build FretParams from UI inputs and dataset background measurement.
     */
    private FretParams buildFretParams(BackgroundService bgService) throws Exception {
        FretBackground background;

        if (backgroundMethod.equals("Manual input")) {
            background = new FretBackground(manualFretBg, manualDonorBg, manualAcceptorBg);
            logService.info("Using manual background values");
        } else {
            // Auto-estimate from data
            logService.info("Auto-estimating background from image...");
            float[][] fretPlane = DatasetUtils.extractPlane(dataset, fretChannel, 0, 0);
            float[][] donorPlane = DatasetUtils.extractPlane(dataset, donorChannel, 0, 0);
            float[][] acceptorPlane = DatasetUtils.extractPlane(dataset, acceptorChannel, 0, 0);
            
            background = bgService.measureBackground(fretPlane, donorPlane, acceptorPlane);
        }

        // Normalize method
        int normMethod = normalizationChoice.equals("FRET/Donor") ? 0 :
                       normalizationChoice.equals("FRET/Acceptor") ? 1 :
                       normalizationChoice.equals("FRET/(D*A)") ? 2 :
                       normalizationChoice.equals("FRET/sqrt(D*A)") ? 3 : 4;

        return new FretParams.Builder()
            .donorChannel(donorChannel)
            .fretChannel(fretChannel)
            .acceptorChannel(acceptorChannel)
            .background(background)
            .gaussianSigma(gaussianSigma)
            .normalizationMethod(normMethod)
            .thresholdFactor(thresholdFactor)
            .build();
    }

    /**
     * Create output datasets from results and display them.
     */
    private void createAndDisplayResults(FretAnalysisResult[][][] results, FretParams params,
                                        ResultsService resultsService) {
        int numTimepoints = results.length;
        int numZSlices = results[0].length;

        logService.info("Creating result stacks (" + numTimepoints + " timepoints, " + numZSlices + " Z-slices)");

        if (numTimepoints == 1 && numZSlices == 1) {
            // Single frame - just display
            FretAnalysisResult result = results[0][0][0];
            displaySingleResult(result, resultsService);
        } else if (numTimepoints > 1 && numZSlices == 1) {
            // Time series
            FretAnalysisResult[] timeSeries = new FretAnalysisResult[numTimepoints];
            for (int t = 0; t < numTimepoints; t++) {
                timeSeries[t] = results[t][0][0];
            }
            displayTimeSeries(timeSeries, resultsService);
        } else if (numTimepoints == 1 && numZSlices > 1) {
            // Z-stack
            FretAnalysisResult[] zstack = new FretAnalysisResult[numZSlices];
            for (int z = 0; z < numZSlices; z++) {
                zstack[z] = results[0][z][0];
            }
            displayZStack(zstack, resultsService);
        } else {
            // 4D data
            display4D(results, resultsService);
        }

        // Log summary statistics
        FretAnalysisResult result = results[0][0][0];
        logService.info(resultsService.generateSummary(result));
    }

    /**
     * Display single frame result.
     */
    private void displaySingleResult(FretAnalysisResult result, ResultsService service) {
        logService.info("Displaying single frame result");
        
        int width = result.getWidth();
        int height = result.getHeight();

        // Create normalized FRET result as Dataset
        float[] normalizedFlat = flattenArray(result.getNormalizedFret());
        Dataset normFretDataset = datasetService.create(
            ArrayImgs.floats(normalizedFlat, width, height));
        normFretDataset.setAxes(new DefaultLinearAxis[]{new DefaultLinearAxis(Axes.X), new DefaultLinearAxis(Axes.Y)});
        normFretDataset.setName(dataset.getName() + " - FRET Normalized");
        uiService.show("FRET Normalized", normFretDataset);

        // Create corrected FRET result as Dataset
        float[] correctedFlat = flattenArray(result.getFretValues());
        Dataset correctedDataset = datasetService.create(
            ArrayImgs.floats(correctedFlat, width, height));
        correctedDataset.setAxes(new DefaultLinearAxis[]{new DefaultLinearAxis(Axes.X), new DefaultLinearAxis(Axes.Y)});
        correctedDataset.setName(dataset.getName() + " - FRET Corrected");
        uiService.show("FRET Corrected", correctedDataset);
    }

    /**
     * Display time-series results.
     */
    private void displayTimeSeries(FretAnalysisResult[] results, ResultsService service) {
        logService.info("Displaying time-series (" + results.length + " timepoints)");
        
        int width = results[0].getWidth();
        int height = results[0].getHeight();
        int numFrames = results.length;

        // Stack normalized FRET values
        float[] normalizedStack = new float[width * height * numFrames];
        for (int t = 0; t < numFrames; t++) {
            float[][] frame = results[t].getNormalizedFret();
            int offset = t * width * height;
            flattenInto(frame, normalizedStack, offset);
        }

        Dataset normDataset = datasetService.create(
            ArrayImgs.floats(normalizedStack, width, height, numFrames));
        normDataset.setAxes(new DefaultLinearAxis[]{new DefaultLinearAxis(Axes.X), new DefaultLinearAxis(Axes.Y), new DefaultLinearAxis(Axes.TIME)});
        normDataset.setName(dataset.getName() + " - FRET Normalized (T-series)");
        uiService.show("FRET Normalized (T)", normDataset);

        // Stack corrected FRET values
        float[] correctedStack = new float[width * height * numFrames];
        for (int t = 0; t < numFrames; t++) {
            float[][] frame = results[t].getFretValues();
            int offset = t * width * height;
            flattenInto(frame, correctedStack, offset);
        }

        Dataset correctedDataset = datasetService.create(
            ArrayImgs.floats(correctedStack, width, height, numFrames));
        correctedDataset.setAxes(new DefaultLinearAxis[]{new DefaultLinearAxis(Axes.X), new DefaultLinearAxis(Axes.Y), new DefaultLinearAxis(Axes.TIME)});
        correctedDataset.setName(dataset.getName() + " - FRET Corrected (T-series)");
        uiService.show("FRET Corrected (T)", correctedDataset);

        logService.info("Time-series display complete");
    }

    /**
     * Display Z-stack results.
     */
    private void displayZStack(FretAnalysisResult[] results, ResultsService service) {
        logService.info("Displaying Z-stack (" + results.length + " slices)");
        
        int width = results[0].getWidth();
        int height = results[0].getHeight();
        int numSlices = results.length;

        // Stack normalized FRET values
        float[] normalizedStack = new float[width * height * numSlices];
        for (int z = 0; z < numSlices; z++) {
            float[][] slice = results[z].getNormalizedFret();
            int offset = z * width * height;
            flattenInto(slice, normalizedStack, offset);
        }

        Dataset zDataset = datasetService.create(
            ArrayImgs.floats(normalizedStack, width, height, numSlices));
        zDataset.setAxes(new DefaultLinearAxis[]{new DefaultLinearAxis(Axes.X), new DefaultLinearAxis(Axes.Y), new DefaultLinearAxis(Axes.Z)});
        zDataset.setName(dataset.getName() + " - FRET Normalized (Z-stack)");
        uiService.show("FRET Normalized (Z)", zDataset);

        // Stack corrected FRET values
        float[] correctedStack = new float[width * height * numSlices];
        for (int z = 0; z < numSlices; z++) {
            float[][] slice = results[z].getFretValues();
            int offset = z * width * height;
            flattenInto(slice, correctedStack, offset);
        }

        Dataset correctedZDataset = datasetService.create(
            ArrayImgs.floats(correctedStack, width, height, numSlices));
        correctedZDataset.setAxes(new DefaultLinearAxis[]{new DefaultLinearAxis(Axes.X), new DefaultLinearAxis(Axes.Y), new DefaultLinearAxis(Axes.Z)});
        correctedZDataset.setName(dataset.getName() + " - FRET Corrected (Z-stack)");
        uiService.show("FRET Corrected (Z)", correctedZDataset);

        logService.info("Z-stack display complete");
    }

    /**
     * Display 4D (time x Z) results.
     */
    private void display4D(FretAnalysisResult[][][] results, ResultsService service) {
        logService.info("Displaying 4D result (time x Z-stack)");
        
        int numTimepoints = results.length;
        int numZSlices = results[0].length;
        int width = results[0][0][0].getWidth();
        int height = results[0][0][0].getHeight();

        float[] normalizedStack = new float[width * height * numZSlices * numTimepoints];
        
        for (int t = 0; t < numTimepoints; t++) {
            for (int z = 0; z < numZSlices; z++) {
                float[][] frame = results[t][z][0].getNormalizedFret();
                int offset = (t * numZSlices + z) * width * height;
                flattenInto(frame, normalizedStack, offset);
            }
        }

        Dataset result4d = datasetService.create(
            ArrayImgs.floats(normalizedStack, width, height, numZSlices, numTimepoints));
        result4d.setAxes(new DefaultLinearAxis[]{new DefaultLinearAxis(Axes.X), new DefaultLinearAxis(Axes.Y), new DefaultLinearAxis(Axes.Z), new DefaultLinearAxis(Axes.TIME)});
        result4d.setName(dataset.getName() + " - FRET Normalized (4D)");
        uiService.show("FRET Normalized (4D)", result4d);

        // Stack corrected FRET values
        float[] correctedStack = new float[width * height * numZSlices * numTimepoints];
        
        for (int t = 0; t < numTimepoints; t++) {
            for (int z = 0; z < numZSlices; z++) {
                float[][] frame = results[t][z][0].getFretValues();
                int offset = (t * numZSlices + z) * width * height;
                flattenInto(frame, correctedStack, offset);
            }
        }

        Dataset corrected4d = datasetService.create(
            ArrayImgs.floats(correctedStack, width, height, numZSlices, numTimepoints));
        corrected4d.setAxes(new DefaultLinearAxis[]{new DefaultLinearAxis(Axes.X), new DefaultLinearAxis(Axes.Y), new DefaultLinearAxis(Axes.Z), new DefaultLinearAxis(Axes.TIME)});
        corrected4d.setName(dataset.getName() + " - FRET Corrected (4D)");
        uiService.show("FRET Corrected (4D)", corrected4d);

        logService.info("4D display complete");
    }

    /**
     * Flatten 2D array into 1D.
     */
    private float[] flattenArray(float[][] array) {
        int height = array.length;
        int width = array[0].length;
        float[] flat = new float[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                flat[y * width + x] = array[y][x];
            }
        }

        return flat;
    }

    /**
     * Flatten 2D array into 1D at specific offset.
     */
    private void flattenInto(float[][] array, float[] flat, int offset) {
        int height = array.length;
        int width = array[0].length;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                flat[offset + y * width + x] = array[y][x];
            }
        }
    }

    public static void main(final String... args) throws Exception {
        // For testing / debugging
        ImageJ ij = new ImageJ();
        
        // Try to open test image if it exists
        File testImg = new File("vidfret_/test-images/testmovie.tiff");
        if (testImg.exists()) {
            Dataset ds = (Dataset) ij.io().open(testImg.getAbsolutePath());
            ij.ui().showUI();
            ij.ui().show(ds);
            System.out.println("Loaded: " + testImg.getAbsolutePath());
            System.out.println("Dimensions - X:" + ds.dimension(Axes.X) + 
                             " Y:" + ds.dimension(Axes.Y) + 
                             " C:" + ds.dimension(Axes.CHANNEL));
            
            // Launch plugin command
            ij.command().run(vidFret_.class, true);
        } else {
            System.err.println("Test image not found: " + testImg.getAbsolutePath());
            System.err.println("Place a multi-channel TIFF in vidfret_/test-images/ to test");
        }
    }

}
