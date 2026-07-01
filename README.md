# vidFRET

Quantitative analysis of time-lapse sensitized FRET (Förster Resonance Energy Transfer). Inspired by [pixFRET](https://imagej.net/plugins/pixfret).

## Overview

vidFRET extends pixFRET's capabilities for multidimensional FRET analysis, with a focus on programmatic and headless usage in ImageJ/Fiji. While pixFRET provides excellent interactive analysis, vidFRET facilitates analysis of multidimensional images and plays nicely with scripting and batch processing.

**Spectral bleed through (SBT)**
vidFRET does not internally facilitate calculation of SBT however correction using known SBT coeffecients is supported.
For example, SBT values may be calculated on an experiment-by-experiment basis using pixFRET. The returned `BTdon` and `BTacc` values returned by pixFRET can then be fed into the startup parameters of vidFRET to allow for corrected FRET output.
Please note, currently only constant SBT values are supported.

### Key Features

- **Multidimensional Support**: Handles time-lapse, Z-stack, and multichannel images
- **Programmatic Interface**: Designed for headless operation and macro scripting
- **Flexible normalisation**: Multiple FRET normalisation methods
- **Background Correction**: Automatic or manual background estimation
- **Gaussian Smoothing**: Optional image preprocessing
- **Auto-thresholding**: Automatic mask generation using various algorithms (Default/Otsu, Mean, Minimum, Triangle)

## Installation

1. Download the most recent JAR file from the [releases](https://github.com/noahbcp/vidfret/releases) or build it with Maven.
2. Place the JAR in your `Fiji.app/plugins/` directory
3. Restart Fiji

```
git clone https://github.com/noahbcp/vidfret.git
mvn package -f "pom.xml"
```
## Usage

### Interactive Mode

Run the plugin through Plugins → vidFRET menu. Configure parameters in the dialog.

### Programmatic/Macro Usage

vidFRET is optimised for headless and scripted use. 
E.g. In ImageJ macros (.ijm):

```javascript
run("vidfret",
    "donorchannel=1 " +
    "fretchannel=2 " +
    "acceptorchannel=0 " +
    "btdonor=0.02" +
    "btacceptor=0.05" +
    "startframe=1 " +
    "endframe=10 " +
    "gaussiansigma=0.5 " +
    "backgroundmethod=[Auto (5th percentile)] " +
    "manualdonorbg=0 " +
    "manualfretbg=0 " +
    "manualacceptorbg=0 " +
    "normalisationchoice=FRET/Donor " +
    "thresholdmethod=Default ");
```

### Parameters

- **donor**: Donor channel number (required)
- **fret**: FRET channel number (required)  
- **acceptor**: Acceptor channel number (optional, let = 0 if not using acceptor-dependent normalisations)
- **BTdonor**: SBT coefficient for the donor channel (0 = no correction)
- **BTacceptor**: SBT coefficient for the acceptor channel (0 = no correction)
- **start**: Starting frame number (default: 1)
- **end**: Ending frame number (default: all frames)
- **gaussian**: Gaussian smoothing sigma in pixels (0 = no smoothing)
- **background**: Background estimation method
  - "Auto (5th percentile)": Automatic from image data
  - "Manual input": Use specified values
- **manual_donor_bg**: Manual donor background value (when using manual background)
- **manual_fret_bg**: Manual FRET background value (when using manual background)
- **manual_acceptor_bg**: Manual acceptor background value (when using manual background)
- **normalisation**: FRET normalisation method (see below)
- **thresholdmethod**: Auto-threshold method for masking (Default/Otsu, Mean, Minimum, Triangle)

### Normalisation Methods

vidFRET supports several FRET normalisation approaches:

- **FRET/Donor**: Raw FRET divided by donor intensity
- **FRET/Acceptor**: Raw FRET divided by acceptor intensity  
- **FRET/(D*A)**: Raw FRET divided by (donor × acceptor)
- **FRET/sqrt(D*A)**: Raw FRET divided by sqrt(donor × acceptor)
- **FRET Efficiency**: Calculated FRET efficiency (requires acceptor channel)

When acceptor channel is not available (set to 0), acceptor values are treated as 1.0 in calculations. This results in unchanged results provided the normalisation method doesn't consider the acceptor channel.

## Output

The plugin generates FRET analysis results as new images:

- **FRET Normalised**: Processed FRET image with chosen normalisation
- **FRET Corrected**: Background-corrected FRET data

For multidimensional inputs, outputs preserve the temporal and spatial dimensions.

## Algorithm

1. Extract donor, FRET, and acceptor (if available) planes
2. Apply Gaussian smoothing if specified
3. Estimate background values
4. Generate auto-threshold mask from first frame's donor channel (Default/Otsu, Mean, Minimum, or Triangle method)
5. Apply mask to all channels (pixels below threshold become 0)
6. Perform bleedthrough correction and normalisation
7. Generate output images (FRET, NFRET).

**Note**: The auto-threshold is calculated once from the first frame and applied consistently across the entire time-series to ensure stable measurements.

## Citation

If you use vidFRET in your research, please cite both the original [pixFRET work](https://doi.org/10.1002/jemt.20215) and vidFRET.
See [citation.cff](citation.cff) for more info.
