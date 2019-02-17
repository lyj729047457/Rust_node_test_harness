package org.aion.harness.util;

import java.util.List;

public interface IEventRequest {

    public boolean isSatisfiedBy(String line, long currentTimeInMillis);

    public boolean isExpiredAtTime(long currentTimeInMillis);

    public void waitForOutcome();

    public void notifyRequestIsResolved();

    public List<String> getAllObservedEvents();

    public long timeOfObservation();

    public long deadline();

    public void markAsRejected(String cause);

    public void markAsUnobserved();

    public void markAsExpired();

    public String getCauseOfRejection();

    public boolean isPending();

    public boolean isRejected();

    public boolean isUnobserved();

    public boolean isSatisfied();

    public boolean isExpired();


}
