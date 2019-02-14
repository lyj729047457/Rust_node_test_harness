package org.aion.harness.result;

public final class RPCResult {
    private final Result result;
    private final String output;

    private RPCResult(Result result, String output) {
        if (result == null) {
            throw new NullPointerException("result cannot be null");
        }
        if (output == null) {
            throw new NullPointerException("output cannot be null");
        }

        this.result = result;
        this.output = output;
    }

    public static RPCResult successful(String output) {
        return new RPCResult(Result.successful(), output);
    }

    public static RPCResult unsuccessful(int status, String error) {
        return new RPCResult(Result.unsuccessful(status, error), "");
    }

    public Result getResultOnly() {
        return this.result;
    }

    public String getOutput() {
        return this.output;
    }

    public String getOutputResult() {
        int startingIndex = this.output.indexOf("result") + 9;
        int endingIndex = this.output.indexOf("id") - 3;
        String result = this.output.substring(startingIndex, endingIndex);
        return (result.startsWith("0x")) ? result.substring(2) : result;
    }

    @Override
    public String toString() {
        return "Result { success = " + this.result.success + ", output = " + this.output
                + ", status = " + this.result.status + ", error = " + this.result.error + " }";
    }
}
