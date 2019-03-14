package org.aion.harness.statistics;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.aion.harness.main.util.TestHarnessHelper;
import org.aion.harness.result.LogEventResult;
import org.aion.harness.result.RpcResult;

public final class DurationStatistics {
    private static final BigDecimal SECONDS_DIVISOR = BigDecimal.TEN.pow(9);
    private final List<BigDecimal> startTimes;
    private final List<BigDecimal> endTimes;

    private boolean statsHaveBeenComputed = false;
    private BigDecimal maximumDuration = null;
    private BigDecimal minimumDuration = null;
    private BigDecimal meanDuration = null;
    private BigDecimal totalDuration = BigDecimal.ZERO;
    private BigDecimal durationsStandardDeviation = null;

    private DurationStatistics(long[] startTimes, long[] endTimes) {
        if (startTimes == null) {
            throw new NullPointerException("Cannot construct statstics for null results.");
        }
        if (endTimes == null) {
            throw new NullPointerException("Cannot construct statistics for null futures.");
        }
        if (startTimes.length != endTimes.length) {
            throw new IllegalArgumentException("Cannot construct statics when results and futures differ in size.");
        }

        this.startTimes = new ArrayList<>();
        this.endTimes = new ArrayList<>();

        int length = startTimes.length;
        for (int i = 0; i < length; i++) {
            this.startTimes.add(BigDecimal.valueOf(startTimes[i]));
            this.endTimes.add(BigDecimal.valueOf(endTimes[i]));
        }

    }

    /**
     * Constructs a duration statistics object from the provided lists of rpc results and log event
     * results.
     *
     * It is assumed that these two results are meaningfully related. That is, that the futures
     * capture the observation of events that were directly triggered by the rpc results.
     *
     * Therefore, this method assumes that rpc results represent the "starting times" and log results
     * represent the "ending times" of the various events.
     *
     * @param rpcResults The "starting" times, held in rpc results.
     * @param logResults The "ending" times, held in log event results.
     */
    public static <T> DurationStatistics from(List<RpcResult<T>> rpcResults, List<LogEventResult> logResults) {
        long[] start = TestHarnessHelper.extractResultTimestamps(rpcResults, TimeUnit.NANOSECONDS);
        long[] end = TestHarnessHelper.extractEventTimestamps(logResults, TimeUnit.NANOSECONDS);

        if (end == null) {
            throw new IllegalArgumentException("Unable to extract the log result timestamps. "
                + "At least one of these results was not observed and has no timestamp!");
        }

        return new DurationStatistics(start, end);
    }

    /**
     * Prints some basic duration statistics to console.
     *
     * These statistics are just the maximum & minimum durations, the mean duration, the total
     * amount of time elapsed between the earliest & latest timestamps, as well as the standard
     * deviation for the durations.
     *
     * @param decimalPrecision The number of decimal places for the numbers to be accurate to.
     */
    public void printStatistics(int decimalPrecision) {
        if (!this.statsHaveBeenComputed) {
            computeStatistics(decimalPrecision);
        }

        System.out.println("---------------------------------------------------------------------");
        System.out.println("Maximum duration: " + this.maximumDuration.toPlainString() + " seconds(s)");
        System.out.println("Minimum duration: " + this.minimumDuration.toPlainString() + " seconds(s)");
        System.out.println("Mean duration: " + this.meanDuration.toPlainString() + " second(s)");
        System.out.println("Total duration: " + this.totalDuration.toPlainString() + " second(s)");
        System.out.println("Standard deviation of durations: " + this.durationsStandardDeviation.toPlainString() + " second(s)");
        System.out.println("---------------------------------------------------------------------");
    }

    private void computeStatistics(int precision) {
        int size = this.startTimes.size();

        BigDecimal earliestTime = null;
        BigDecimal latestTime = null;

        List<BigDecimal> durations = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            BigDecimal currentResultTime = this.startTimes.get(i);
            BigDecimal currentFutureTime = this.endTimes.get(i);

            earliestTime = (earliestTime == null) ? currentResultTime : ((earliestTime.compareTo(currentResultTime) < 0) ? earliestTime : currentResultTime);
            latestTime = (latestTime == null) ? currentFutureTime : ((latestTime.compareTo(currentFutureTime) > 0) ? latestTime : currentFutureTime);

            BigDecimal duration = currentFutureTime.subtract(currentResultTime);

            this.maximumDuration = (this.maximumDuration == null) ? duration : ((this.maximumDuration.compareTo(duration) < 0) ? duration : this.maximumDuration);
            this.minimumDuration = (this.minimumDuration == null) ? duration : ((this.minimumDuration.compareTo(duration) > 0) ? duration : this.minimumDuration);

            durations.add(duration.divide(SECONDS_DIVISOR, precision, RoundingMode.HALF_UP));
        }

        // In this case, we must have had a size zero list.
        if (earliestTime == null) {
            this.totalDuration = BigDecimal.ZERO;
            this.maximumDuration = BigDecimal.ZERO;
            this.minimumDuration = BigDecimal.ZERO;
            this.meanDuration = BigDecimal.ZERO;
            this.durationsStandardDeviation = BigDecimal.ZERO;
        } else {
            this.totalDuration = latestTime.subtract(earliestTime).divide(SECONDS_DIVISOR, precision, RoundingMode.HALF_UP);
            this.maximumDuration = this.maximumDuration.divide(SECONDS_DIVISOR, precision, RoundingMode.HALF_UP);
            this.minimumDuration = this.minimumDuration.divide(SECONDS_DIVISOR, precision, RoundingMode.HALF_UP);

            // The mean is calculated over the individual durations, not the length of time (totalDuration).
            BigDecimal durationsTotal = sum(durations);
            this.meanDuration = durationsTotal.divide(BigDecimal.valueOf(size), precision, RoundingMode.HALF_UP);

            this.durationsStandardDeviation = computeStandardDeviation(durations, precision);
        }

        this.statsHaveBeenComputed = true;
    }

    private BigDecimal computeStandardDeviation(List<BigDecimal> times, int precision) {
        BigDecimal squares = BigDecimal.ZERO;

        for (BigDecimal time : times) {
            squares = squares.add((time.subtract(this.meanDuration)).pow(2));
        }

        BigDecimal variance = squares.divide(this.meanDuration, precision, RoundingMode.HALF_UP);
        return variance.sqrt(new MathContext(precision, RoundingMode.HALF_UP));
    }

    private BigDecimal sum(List<BigDecimal> numbers) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal number : numbers) {
            sum = sum.add(number);
        }
        return sum;
    }

}
