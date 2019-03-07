package org.aion.harness.integ.resources;

import java.io.File;

public final class TarFileFinder {

    /**
     * Returns the pack directory of the aion project whose root is given by kernelSourceDir.
     */
    public static File getPackDirectory(String kernelSourceDir) {
        return new File(kernelSourceDir + File.separator + "pack");
    }

}
