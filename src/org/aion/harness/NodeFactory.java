package org.aion.harness;

import java.util.NoSuchElementException;

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
