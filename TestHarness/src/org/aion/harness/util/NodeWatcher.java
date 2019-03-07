package org.aion.harness.util;

import java.util.HashMap;
import java.util.Map;

public class NodeWatcher {
    private static int nodeIDCounter = 0;

    private Map<Integer, LogReader> nodeToLogReaderPair = new HashMap<>();

    /**
     *  Returns the LogReader corresponding to the given node ID.
     */
    public LogReader getReaderForNodeByID(int ID) {
        return this.nodeToLogReaderPair.get(ID);
    }

    /**
     * Add a log reader, new node id is returned to the caller.
     * The caller is a node, and the returned ID will become the caller's identity.
     */
    public int addReader(LogReader logReader) {
        if (logReader == null) {
            throw new NullPointerException("log reader cannot be null");
        }
        if (this.nodeToLogReaderPair.containsValue(logReader)) {
            throw new IllegalArgumentException("this log reader is already in the watcher");
        }

        int newID = nodeIDCounter++;
        this.nodeToLogReaderPair.put(newID, logReader);

        return newID;
    }

    /**
     * Remove node ID and LogReader pair from the map.
     */
    public void removeReader(int nodeID) {
        this.nodeToLogReaderPair.remove(nodeID);
    }

    /**
     * Reset the map.
     */
    public void resetNodeWatcher() {
        this.nodeToLogReaderPair.clear();
    }

    /**
     * Returns the number of node ID to LogReader pairs in the map.
     */
    public int numberOfNodesWatched() {
        return this.nodeToLogReaderPair.size();
    }

}
