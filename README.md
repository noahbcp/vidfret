# vidFRET

Quantitative analysis of time-lapse sensitized FRET (Förster Resonance Energy Transfer). Inspired by [pixFRET](https://imagej.net/plugins/pixfret).

## Overview

vidFRET extends pixFRET's capabilities for multidimensional FRET analysis, with a focus on programmatic and headless usage in ImageJ/Fiji. While pixFRET provides excellent interactive analysis, vidFRET enables automated processing of large datasets, time-series, and complex multidimensional images through scripting and batch processing.

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

## Installation

1. Download the JAR file from the releases
2. Place in `Fiji.app/plugins/` directory
3. Restart Fiji

## Usage

### Interactive Mode

Run the plugin through Plugins → vidFRET menu. Configure parameters in the dialog.

### Programmatic/Macro Usage

vidFRET is optimised for headless and scripted use. In ImageJ macros (.ijm):

```javascript
run("vidFret",
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
    "thresholdfactor=1.0");
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
- **threshold**: Threshold factor for masking low-signal pixels

### normalisation Methods

vidFRET supports several FRET normalisation approaches:

- **FRET/Donor**: Raw FRET divided by donor intensity
- **FRET/Acceptor**: Raw FRET divided by acceptor intensity  
- **FRET/(D*A)**: Raw FRET divided by (donor × acceptor)
- **FRET/sqrt(D*A)**: Raw FRET divided by sqrt(donor × acceptor)
- **FRET Efficiency**: Calculated FRET efficiency (requires acceptor channel)

When acceptor channel is not available (set to 0), acceptor values are treated as 1.0 in calculations. This results in unchanged results provided the normalisation method use doesn't consider the acceptor channel.

## Output

The plugin generates FRET analysis results as new images:

- **FRET Normalized**: Processed FRET image with chosen normalisation
- **FRET Corrected**: Background-corrected FRET data

For multidimensional inputs, outputs preserve the temporal and spatial dimensions.

## Algorithm

1. Extract donor, FRET, and acceptor (if available) planes
2. Apply Gaussian smoothing if specified
3. Estimate background values
4. Perform bleedthrough correction and normalisation
5. Apply threshold masking for low-signal regions
6. Generate output images with proper axis labeling

## Citation

If you use vidFRET in your research, please cite both the original [pixFRET work](https://doi.org/10.1002/jemt.20215) and vidFRET.

