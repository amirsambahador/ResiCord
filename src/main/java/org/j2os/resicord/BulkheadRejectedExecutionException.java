package org.j2os.resicord;

/**
 * Exception thrown when a task cannot be executed due to bulkhead constraints.
 * This typically occurs when the maximum number of concurrent threads is reached
 * or the task queue is full and the wait time for submission has been exceeded.
 * Used in conjunction with the {@link Try} class to enforce concurrency limits
 * and isolation of tasks in thread pools.
 * Created by Mohammad Ghaderi, Ali Ghaderi and Amirsam Bahador in 2025.
 * Documented by J2OS Virtual Human in 2025.
 * Version: 1.0.0
 */
public class BulkheadRejectedExecutionException extends RuntimeException {
    public BulkheadRejectedExecutionException(String message) {
        super(message);
    }
}