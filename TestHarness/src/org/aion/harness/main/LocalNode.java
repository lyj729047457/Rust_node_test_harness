package org.aion.harness.main;

import org.aion.harness.main.types.Network;
import org.aion.harness.main.types.NodeConfigurations;
import org.aion.harness.result.Result;

public interface LocalNode extends Node {

    /**
     * Configures the node with the specified settings.
     *
     * This method must be called prior to any other actionable method in this class.
     *
     * @param configurations the configuration settings.
     */
    public void configure(NodeConfigurations configurations);

    /**
     * Returns the network that this node will attempt to connect to when {@code start()} is invoked,
     * or, if the node is running, then the network it has connected to.
     *
     * Returns null if no network has been configured yet.
     *
     * @return the network the node is on.
     */
    public Network getNetwork();

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
     * @throws IllegalStateException if the node has not been configured yet.
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
     * @throws IllegalStateException if the node has not been configured yet.
     */
    public Result initializeKernel();

    /**
     * Grabs a built kernel and brings it into the working directory, doing whatever other
     * preparation is necessary so that the kernel is ready to be used after this method returns.
     *
     * If there is an original node directory, and it contains a database for the specified network, then that database
     * is reused.
     *
     * If this method is successful, then {@code start()} can be called.
     *
     * If there is no built kernel or the kernel source code has been updated, a call to
     * {@code buildKernel()} should be made prior to this method.
     *
     * Displays the I/O of any external processed launched as well as additional information.
     *
     * @return a result indicating the success or failure of this method.
     * @throws IllegalStateException if the node has not been configured yet.
     */
    Result initializeKernelAndPreserveDatabaseVerbose();

    /**
     * Grabs a built kernel and brings it into the working directory, doing whatever other
     * preparation is necessary so that the kernel is ready to be used after this method returns.
     *
     * If there is an original node directory, and it contains a database for the specified network, then that database
     * is reused.
     *
     * If this method is successful, then {@code start()} can be called.
     *
     * If there is no built kernel or the kernel source code has been updated, a call to
     * {@code buildKernel()} should be made prior to this method.
     *
     * @return a result indicating the success or failure of this method.
     * @throws IllegalStateException if the node has not been configured yet.
     */
    Result initializeKernelAndPreserveDatabase();

    /**
     * Builds the kernel from source.
     *
     * Displays the I/O of the build process.
     *
     * @return a result indicating the success of failure of this method.
     * @throws IllegalStateException if the node has not been configured yet.
     */
    public Result buildKernelVerbose();

    /**
     * Builds the kernel from source.
     *
     * @return a result indicating the success of failure of this method.
     * @throws IllegalStateException if the node has not been configured yet.
     */
    public Result buildKernel();

    /**
     * Starts running the node.
     *
     * @return a result indicating the success of failure of this method.
     * @throws IllegalStateException if the node has not been configured yet.
     */
    public Result start();

    /**
     * Shuts the node down.
     *
     * @return a result indicating the success of failure of this method.
     * @throws IllegalStateException if the node has not been configured yet.
     */
    public Result stop();

    /**
     * Returns {@code true} if node is running, {@code false} otherwise.
     *
     * @return true only if this node is running.
     */
    public boolean isAlive();

    /**
     * Resets the node's database.
     *
     * Typically this method is only safe to call if {@code isAlive() == false}.
     *
     * @return a result indicating the success of failure of this method.
     * @throws IllegalStateException if the node has not been configured yet.
     */
    public Result resetState();

}
