package org.aion.harness.util;

import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.Result;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

import java.util.*;
import java.util.concurrent.TimeUnit;

public final class LogListener implements TailerListener {
    private static final Object WAIT_MONITOR = new Object();

    private static final int CAPACITY = 10;

    private List<EventRequest> requests = new ArrayList<>(CAPACITY);

    public void waitForRequest() throws InterruptedException {
        synchronized (WAIT_MONITOR) {
            WAIT_MONITOR.wait(TimeUnit.MILLISECONDS.toSeconds(Assumptions.REQUEST_MANAGER_TIMEOUT_MILLIS));
        }
    }

    public Result submit(EventRequest eventRequest) {
        return addToList(eventRequest);
    }

    @Override
    public void init(Tailer tailer) {
        //TODO
    }

    @Override
    public void fileNotFound() {
        System.err.println("FILE NOT FOUND");
    }

    @Override
    public void fileRotated() {
        System.err.println("FILE ROTATED!");
    }

    @Override
    public void handle(String nextLine) {
        if (this.requests.isEmpty()) {
            return;
        }

        List<NodeEvent> eventsToListenFor = gatherAllEvents();
        NodeEvent event = checkIfEventOccurred(eventsToListenFor, nextLine);
        if (event != null) {
            answerRequests(event);
        }
    }

    @Override
    public void handle(Exception e) {
        e.printStackTrace();
    }

    private List<NodeEvent> gatherAllEvents() {
        List<NodeEvent> distinctEvents = new ArrayList<>();
        for (EventRequest request : this.requests) {
            if (!distinctEvents.contains(request.getRequest())) {
                distinctEvents.add(request.getRequest());
            }
        }
        return distinctEvents;
    }

    private void answerRequests(NodeEvent discoveredEvent) {
        for (EventRequest request : this.requests) {
            if ((!request.hasResult()) && (request.getRequest().equals(discoveredEvent))) {
                request.addResult(Result.successful());
                System.err.println("Answering request: " + request);
            }
        }

        removeFromList(discoveredEvent);
    }

    private void removeFromList(NodeEvent event) {
        synchronized (this) {
            this.requests.removeIf(request -> request.getRequest().equals(event));
        }

        synchronized (WAIT_MONITOR) {
            WAIT_MONITOR.notifyAll();
        }
    }

    private Result addToList(EventRequest eventRequest) {
        synchronized (this) {
            if (this.requests.size() == CAPACITY) {
                return Result.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "Maximum number of requests being processed.");
            }

            System.err.println("Adding request to list: " + eventRequest);
            this.requests.add(eventRequest);
            return Result.successful();
        }
    }

    private NodeEvent checkIfEventOccurred(List<NodeEvent> events, String line) {
        for (NodeEvent event : events) {
            if (line.contains(event.getEventString())) {
                return event;
            }
        }
        return null;
    }

}
