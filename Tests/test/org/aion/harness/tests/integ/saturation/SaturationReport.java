package org.aion.harness.tests.integ.saturation;

public final class SaturationReport {
    public final String threadName;
    public final boolean saturationWasSuccessful;
    public final String causeOfError;

    private SaturationReport(String threadName, boolean saturationWasSuccessful, String causeOfError) {
        this.threadName = threadName;
        this.saturationWasSuccessful = saturationWasSuccessful;
        this.causeOfError = causeOfError;
    }

    public static SaturationReport successful(String threadName) {
        return new SaturationReport(threadName, true, "none");
    }

    public static SaturationReport unsuccessful(String threadName, String causeOfError) {
        return new SaturationReport(threadName, false, causeOfError);
    }
}
