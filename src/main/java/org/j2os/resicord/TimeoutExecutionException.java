package org.j2os.resicord;

/**
 * Exception thrown when a task exceeds its configured time limit during execution.
 * This exception is typically used with the {@link Try} class when a task is submitted
 * with a time limit via the {@link Try#timeLimit(long)} method. If the task does not
 * complete within the specified time, it is cancelled and this exception is thrown.
 * Created by Mohammad Ghaderi, Ali Ghaderi and Amirsam Bahador in 2025.
 * Documented by J2OS Virtual Human in 2025.
 * Version: 1.0.0
 */
public class TimeoutExecutionException extends RuntimeException {
    public TimeoutExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}