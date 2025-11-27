package com.github.systeminvecklare.libs.simpleproperties.result;

/**
 * Represents the outcome of an operation, either success or failure.
 * <p>
 * A successful result has {@link #success} set to {@code true} and
 * {@link #errorMessage} as {@code null}. A failed result has {@link #success}
 * set to {@code false} and provides an error message describing the failure.
 * </p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * Result r1 = Result.success();          // successful outcome
 * Result r2 = Result.error("Something went wrong"); // failed outcome
 * if (!r2.success) {
 *     System.out.println(r2.errorMessage);
 * }
 * }</pre>
 */
public class Result {
    private static final Result SUCCESS = new Result(true, null);

    /** True if the operation was successful, false otherwise. */
    public final boolean success;

    /** Error message if the operation failed; {@code null} if successful. */
    public final String errorMessage;

    private Result(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    /**
     * Returns a pre-constructed successful result.
     *
     * @return a {@link Result} representing success
     */
    public static Result success() {
        return SUCCESS;
    }

    /**
     * Creates a failed result with the given error message.
     *
     * @param message the error message describing the failure
     * @return a {@link Result} representing failure
     */
    public static Result error(String message) {
        return new Result(false, message);
    }
}
