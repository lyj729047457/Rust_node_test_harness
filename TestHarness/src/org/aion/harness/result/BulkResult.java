package org.aion.harness.result;

import java.util.Collections;
import java.util.List;

/**
 * A result that holds a bulk list of results iff the result is successful.
 *
 * Otherwise, if the result is not successful, then results is an empty list and an error
 * message will be set.
 */
public class BulkResult<T> {
    private final boolean success;
    private final String error;
    private final List<T> results;

    private BulkResult(boolean success, String error, List<T> results) {
        if (error == null) {
            throw new NullPointerException("Cannot construct a BulkResult with a null error.");
        }
        if (results == null) {
            throw new NullPointerException("Cannot construct a BulkResult with null results.");
        }

        this.success = success;
        this.error = error;
        this.results = results;
    }

    /**
     * Constrcuts a successful result using the provided results.
     *
     * @param results The results.
     * @return a successful result with the specified transactions.
     */
    public static <T> BulkResult<T> successful(List<T> results) {
        return new BulkResult<>(true, "", results);
    }

    /**
     * Constrcuts an unsuccessful result using the provided error message.
     *
     * @param error The error message.
     * @return an unsuccessful result.
     */
    public static <T> BulkResult<T> unsuccessful(String error) {
        return new BulkResult<>(false, error, Collections.emptyList());
    }

    /**
     * Returns {@code true} if, and only if, the action that constructed the results was
     * successful. In this case, {@code getResults()} will return all of the results.
     *
     * Otherwise returns {@code false}.
     *
     * @return whether the result-constructing action was successful.
     */
    public boolean isSuccess() {
        return this.success;
    }

    /**
     * Returns the error message if one exists. An error message exists only if
     * {@code isSuccess() == false}.
     *
     * @return the error message.
     */
    public String getError() {
        return this.error;
    }

    /**
     * Returns the results that were constructed. This list is possibly non-empty only if
     * {@code isSuccess == true}.
     *
     * @return The transactions.
     */
    public List<T> getResults() {
        return this.results;
    }

    @Override
    public String toString() {
        if (this.success) {
            return "BulkResult { successful, holds " + this.results.size() + " results }";
        } else {
            return "BulkResult { unsuccessful due to: " + this.error + " }";
        }
    }

}
