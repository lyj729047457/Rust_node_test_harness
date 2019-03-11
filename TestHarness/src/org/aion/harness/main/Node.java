package org.aion.harness.main;

import org.aion.harness.main.types.Network;
import org.aion.harness.main.types.NodeConfigurations;
import org.aion.harness.result.Result;

/**
 * An Aion node.
 */
public interface Node {

    /**
     * Returns the unique identity of this node.
     *
     * @return the node's identity.
     */
    public int getID();

}
