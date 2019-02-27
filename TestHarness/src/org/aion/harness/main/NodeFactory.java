package org.aion.harness.main;

import java.util.NoSuchElementException;
import org.aion.harness.main.impl.JavaNode;

/**
 * A factory for producing {@link Node} implementations.
 *
 * A NodeFactory is not thread-safe.
 */
public final class NodeFactory {

    public enum NodeType { JAVA_NODE }

    public static Node getNewNodeInstance(NodeType node) {
        if (node == null) {
            throw new NullPointerException("Cannot get null node.");
        }

        switch (node) {
            case JAVA_NODE: return new JavaNode();
            default: throw new NoSuchElementException("The provided node type is not yet supported: " + node);
        }
    }

}
