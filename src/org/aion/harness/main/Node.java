package org.aion.harness.main;

import org.aion.harness.result.Result;
import org.aion.harness.result.StatusResult;
import java.io.IOException;

public interface Node {

    /**
     * Grabs a built kernel and brings it into the working directory, doing whatever other
     * preparation is necessary so that the kernel is ready to be used after this method returns.
     *
     * If this method is successful, then {@code start()} can be called.
     *
     * If there is no built kernel or the kernel source code has been updated, a call to
     * {@code buildKernel()} should be made prior to this method.
     *
     * Displays the I/O of any external processed launched as well as additional information.
     *
     * @return a result indicating the success or failure of this method.
     */
    public Result initializeKernelVerbose();

    /**
     * Grabs a built kernel and brings it into the working directory, doing whatever other
     * preparation is necessary so that the kernel is ready to be used after this method returns.
     *
     * If this method is successful, then {@code start()} can be called.
     *
     * If there is no built kernel or the kernel source code has been updated, a call to
     * {@code buildKernel()} should be made prior to this method.
     *
     * @return a result indicating the success or failure of this method.
     */
    public Result initializeKernel();

    /**
     * Builds the kernel from source.
     *
     * Displays the I/O of the build process.
     *
     * @return a result indicating the success of failure of this method.
     */
    public Result buildKernelVerbose();

    /**
     * Builds the kernel from source.
     *
     * @return a result indicating the success of failure of this method.
     */
    public Result buildKernel();

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
    public Result resetState();
}
