package net.noahbcp;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.noahbcp.vidFret.ChannelRole;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.UIService;

import io.scif.services.DatasetIOService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Plugin(type = Command.class, menuPath = "Plugins>vidFret")
public class vidFret<T extends RealType<T>> implements Command {
    /* Parameters */
    @Parameter
    private UIService uiService;

    // Processes currently active image
    @Parameter(label = "Input image", required = true)
    private Dataset dataset;

    // Set channel roles
    @Parameter(label = "Donor channel", min = "1", required = true)
    private Integer donorChannel;

    @Parameter(label = "Acceptor channel", min = "1", required = false)
    private Integer acceptorChannel;

    @Parameter(label = "FRET channel", min = "1", required = true)
    private Integer fretChannel;

    // Main
    @Override
    public void run() {
        /* Input validation */
        Integer channelAxis = dataset.dimensionIndex(Axes.CHANNEL); // Base-0
        if (channelAxis < 1) {
            uiService.showDialog(
                    "The selected image doesn't have enough channels!\nPlease select a multi-channel image.",
                    "Invalid input",
                    DialogPrompt.MessageType.ERROR_MESSAGE);
            return;
        }

    }

    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }

}
