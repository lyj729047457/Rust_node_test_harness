package org.aion.harness.integ.resources;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.aion.harness.main.NodeListener;
import org.aion.harness.result.EventRequestResult;

public final class Eavesdropper implements Runnable {
    private NodeListener listener;
    private Gossip gossip;
    private int ID;
    private AtomicBoolean dead = new AtomicBoolean(false);

    private AtomicBoolean freezeResult = new AtomicBoolean(false);
    private EventRequestResult latestResult = null;

    public enum Gossip {

        // Listen for the node's heartbeat.
        HEARTBEAT,

        // Listen for an event that will never occur.
        UNSPEAKABLE

    }

    private Eavesdropper(Gossip gossip, NodeListener listener, int ID) {
        this.listener = listener;
        this.gossip = gossip;
        this.ID = ID;
    }

    public static Eavesdropper createEavesdropperThatListensFor(Gossip gossip, int ID) {
        return new Eavesdropper(gossip, new NodeListener(), ID);
    }

    @Override
    public void run() {
        while (!this.dead.get()) {

            long startTime = 0;
            EventRequestResult result = null;

            if (this.gossip == Gossip.HEARTBEAT) {

                // Listen for a regular heartbeat event.
                startTime = System.nanoTime();
                result = this.listener.waitForHeartbeat(TimeUnit.MINUTES.toMillis(2));

                if (result == null) {
                    System.err.println("Result is null - unsupported gossip event specified!");
                    this.dead.set(true);
                } else if (result.eventWasObserved()) {
                    long time = TimeUnit.NANOSECONDS.toSeconds(result.timeOfObservationInMilliseconds() - startTime);
                    System.out.println("Thread #" + this.ID + ": event observed | time: " + time + " second(s)");
                } else if (result.eventWasRejected()) {
                    System.out.println("Thread #" + this.ID + ": event request rejected due to: " + result.causeOfRejection());
                } else {
                    System.out.println("Thread #" + this.ID + ": event unobserved.");
                }

            } else if (this.gossip == Gossip.UNSPEAKABLE) {

                // Listen for an event that will never occur.
                startTime = System.nanoTime();
                result = this.listener.waitForLine("I do not exist.", TimeUnit.HOURS.toMillis(1));

            }

            if (!this.dead.get()) {
                if ((this.latestResult == null) || (!this.freezeResult.get())) {
                    synchronized (this) {
                        this.latestResult = result;
                    }
                }
            }

        }
    }

    public void kill() {
        this.dead.set(true);
    }

    public boolean isAlive() {
        return !this.dead.get();
    }

    /**
     * The eavesdropper will not update the latest result anymore, unless it is currently null.
     *
     * Therefore if there is no latest result yet, once the first result comes in it will get frozen
     * and not be updated. If there is a result, then it will just get frozen.
     */
    public void freezeLatestResult() {
        this.freezeResult.set(true);
    }

    /**
     * The eavesdropper will update the latest result every time a new result is produced.
     */
    public void unfreezeLatestResult() {
        this.freezeResult.set(false);
    }

    public synchronized EventRequestResult fetchLatestResult() {
        return this.latestResult;
    }

}
