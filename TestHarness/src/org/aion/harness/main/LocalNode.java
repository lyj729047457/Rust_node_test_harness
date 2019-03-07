package org.aion.harness.main;

import java.io.IOException;
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

    public Result initialize() throws IOException, InterruptedException;

    public Result initializeVerbose() throws IOException, InterruptedException;

    /**
     * Starts running the node.
     *
     * @return a result indicating the success of failure of this method.
     * @throws IllegalStateException if the node has not been configured yet.
     */
    public Result start() throws IOException, InterruptedException;

    /**
     * Shuts the node down.
     *
     * @return a result indicating the success of failure of this method.
     * @throws IllegalStateException if the node has not been configured yet.
     */
    public Result stop() throws IOException, InterruptedException;

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
    public Result resetState() throws IOException;

}
