package org.aion.harness.main.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.aion.harness.result.Result;
import org.aion.harness.util.SimpleLog;

public class RustNodeWithMiner extends RustNode {
    private Process proc = null;

    private static final SimpleLog log = new SimpleLog(RustNodeWithMiner.class.getName());

    @Override
    public Result start() throws IOException, InterruptedException {
        File minerWd = setupMinerProgram();
        ProcessBuilder pb = new ProcessBuilder()
            .directory(minerWd)
            .command("./aionrminer", "-l", "localhost:3333", "-t", "1", "-u", "0xa0d6dec327f522f9c8d342921148a6c42f40a3ce45c1f56baa7bfa752200d9e5");
        proc = pb.start();
        return super.start();
    }

    @Override
    public Result stop() throws IOException, InterruptedException {
        proc.destroy();
        if(! proc.waitFor(1, TimeUnit.MINUTES)) {
            log.log(String.format("Miner failed to stop after the allotted time (pid %d)",
                proc.pid()));
        }
        return super.stop();
    }

    /**
     * Get the directory containing the resource called {@code /mining/}.
     *
     * If it is inside a jar, it will be copied to a temp dir in the filesystem, and
     * the returned value is the temp dir.
     *
     * Otherwise, the resource is already on the filesystem; in which case, the
     * path to the resource is returned.
     *
     * @return a directory that contains aionminer binary */
    private static File setupMinerProgram() throws IOException {
        // aionminer binary is a resource.  Things get a little annoying because
        // we don't know if the resource is going to be inside a jar or just on the
        // filesystem.  If it's in a jar, it needs to be put on to the filesystem
        // otherwise we can't actually start a process using it.
        File maybeMiningDir = new File(
            RustNodeWithMiner.class.getResource("/mining").getPath());

        if(maybeMiningDir.exists()) {
            // filesystem case
            log.log("Found aionrminer at: " + maybeMiningDir);
            return maybeMiningDir;
        } else {
            // jar case
            log.log("aionrminer is in jar; copying it to mining/aionrminer");
            try (InputStream is =
                RustNodeWithMiner.class.getResourceAsStream("/mining/aionrminer")) {

                File miningDir = new File("mining");
                if(! miningDir.exists()) {
                    miningDir.mkdir();
                }
                miningDir.deleteOnExit();
                streamToTempFile(is, "mining/aionrminer");
                return miningDir;
            }
        }
    }

    /**
     * Save input stream into a temp file
     *
     * @param in input stream
     * @param filename temp output filename
     * @return path to the file
     * @throws IOException if IO error
     */
    private static File streamToTempFile(InputStream in, String filename) throws IOException {
        File libFile = new File(filename);
        try (
            final FileOutputStream fos = new FileOutputStream(libFile);
            final OutputStream out = new BufferedOutputStream(fos);
        ) {
            int len = 0;
            byte[] buffer = new byte[8192];
            while ((len = in.read(buffer)) > -1)
                out.write(buffer, 0, len);
        }

        libFile.setExecutable(true);
        libFile.deleteOnExit();
        return libFile;
    }
}
