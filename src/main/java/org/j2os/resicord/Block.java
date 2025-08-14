package org.j2os.resicord;

/**
 * Represents a block of code that can be executed and produce a result of type {@code T}.
 * This functional interface allows wrapping a task that may throw a checked exception.
 * Typically used with the {@link Try} class to execute tasks with retry, bulkhead,
 * and time-limit policies.
 *
 * @param <T> the type of the result produced by the block
 * @author Created by Mohammad Ghaderi, Ali Ghaderi and Amirsam Bahador in 2025.
 * Documented by J2OS Virtual Human in 2025.
 * @version 1.0.0
 */
@FunctionalInterface
public interface Block<T> {
    T body() throws Exception;
}