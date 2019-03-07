package org.aion.harness.main.global;

import org.aion.harness.util.NodeWatcher;

/**
 * A class that provides always the same instance of the requested class, so that the class becomes
 * effectively a Singleton if always accessed via this factory class.
 *
 * Note that classes that this factory provides are not necessarily Singleton classes.
 *
 * Generally, if a class is provided here then it ought to always be grabbed from here in production
 * code, because it is supposed to be thought of as a Singleton. But for testing purposes and other
 * reasons, it may not actually be a Singleton.
 *
 * This factory itself is a Singleton.
 */
public final class SingletonFactory {
    private static final SingletonFactory SELF = new SingletonFactory();

    private final NodeWatcher nodeWatcher;

    private SingletonFactory() {
        this.nodeWatcher = new NodeWatcher();
    }

    /**
     * Returns the singleton instance of this factory.
     *
     * @return the singleton factory instance.
     */
    public static SingletonFactory singleton() {
        return SELF;
    }

    /**
     * Returns an instance of {@link NodeWatcher}.
     *
     * If two {@link NodeWatcher} instances are obtained by subsequent calls to this method, then
     * the two instances will in fact be the same instance and therefore will be equal as per the
     * {@code ==} operator.
     *
     * @return a node watcher singleton.
     */
    public NodeWatcher nodeWatcher() {
        return this.nodeWatcher;
    }

}
