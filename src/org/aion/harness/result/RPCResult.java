package org.aion.harness.result;

public final class RPCResult {
    private final Result result;
    private final String output;
    private final long timestamp;

    private RPCResult(Result result, String output, long timestamp) {
        if (result == null) {
            throw new NullPointerException("result cannot be null");
        }
        if (output == null) {
            throw new NullPointerException("output cannot be null");
        }

        this.result = result;
        this.output = output;
        this.timestamp = timestamp;
    }

    public static RPCResult successful(String output, long timestamp) {
        if (timestamp < 0) {
            throw new IllegalArgumentException("timestamp cannot be less than zero");
        }
        return new RPCResult(Result.successful(), output, timestamp);
    }

    public static RPCResult unsuccessful(int status, String error) {
        return new RPCResult(Result.unsuccessful(status, error), "", -1);
    }

    public Result getResultOnly() {
        return this.result;
    }

    public String getOutput() {
        return this.output;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public String getOutputResult() {
        int startingIndex = this.output.indexOf("result") + 9;
        int endingIndex = this.output.indexOf("id") - 3;
        String result = this.output.substring(startingIndex, endingIndex);
        return (result.startsWith("0x")) ? result.substring(2) : result;
    }

    @Override
    public String toString() {
        return "RPCResult { success = " + this.result.success + ", output = " + this.output
                + ", status = " + this.result.status + ", error = " + this.result.error + " }";
    }
}
