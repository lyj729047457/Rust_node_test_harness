package org.aion.harness.main;

import org.aion.harness.result.Result;
import org.aion.harness.result.StatusResult;
import java.io.IOException;

public interface Node {

    /**
     * Creates a directory named "node" in the current working directory, and places the kernel inside that directory.
     *
     * Returns a result indicating the success or failure of this method.
     */
    StatusResult initialize() throws IOException, InterruptedException;

    /**
     * Creates a directory named "node" in the current working directory, and places the kernel inside that directory.
     *
     * Displays the I/O of the initialization processes.
     *
     * Returns a result indicating the success or failure of this method.
     */
    StatusResult initializeVerbose() throws IOException, InterruptedException;

    /**
     * Starts running the node.
     */
    public Result start();

    /**
     * Shuts the node down.
     */
    public Result stop();

    /**
     * Returns true if node is running, false otherwise.
     */
    public boolean isAlive();

    /**
     * Resets the node's database
     */
    public void resetState() throws IOException;
}
