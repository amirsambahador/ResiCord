package org.j2os.resicord;

/**
 * Provides default configuration values for thread pools used in the {@link Try} class.
 * This interface defines constants for:
 * <ul>
 *     <li>{@link #DEFAULT_POOL_ID} – default identifier for a pool</li>
 *     <li>{@link #MAX_CONCURRENT_THREADS} – maximum number of concurrent threads</li>
 *     <li>{@link #MAX_QUEUE_SIZE} – maximum number of tasks allowed in the queue</li>
 *     <li>{@link #MAX_QUEUE_WAIT_MILLIS} – maximum wait time for task submission in milliseconds</li>
 * </ul>
 * These defaults are used when no explicit configuration is provided by the user.
 * Created by Mohammad Ghaderi, Ali Ghaderi and Amirsam Bahador in 2025.
 * Documented by J2OS Virtual Human in 2025.
 * Version: 1.0.0
 */
public interface DefaultPoolConfig {
    String DEFAULT_POOL_ID = "Default-Pool-Id";
    int MAX_CONCURRENT_THREADS = Integer.MAX_VALUE;
    int MAX_QUEUE_SIZE = Integer.MAX_VALUE;
    long MAX_QUEUE_WAIT_MILLIS = Long.MAX_VALUE;
}
