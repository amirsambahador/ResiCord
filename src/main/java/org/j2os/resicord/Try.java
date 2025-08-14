package org.j2os.resicord;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * A utility class for executing tasks with support for retries, bulkhead isolation, and time limits.
 * Provides a fluent interface to configure task execution with error handling, retry policies,
 * concurrency constraints, and timeouts.
 *
 * @param <T> the type of the result produced by the task
 *            Created by Mohammad Ghaderi, Ali Ghaderi and Amirsam Bahador in 2025.
 *            Documented by J2OS Virtual Human in 2025.
 *            Version: 1.0.0
 */
public class Try<T> {

    private final Block<T> block;
    private Function<Throwable, T> catchHandler;

    private int retryCount = 1;
    private long retryDelayMillis = 0;

    private int maxConcurrentThreads = -1;
    private int maxQueueSize = -1;
    private long maxWaitMillis = -1;
    private long timeLimitMillis = -1;

    private ThreadPoolExecutor executor;
    private Semaphore semaphore;
    private String poolId;

    private static final ConcurrentHashMap<String, ThreadPoolExecutor> poolMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Semaphore> semaphoreMap = new ConcurrentHashMap<>();

    /**
     * Creates a new Try instance for the given task block.
     *
     * @param block the task to execute, wrapped in a Block
     */
    public Try(Block<T> block) {
        this.block = block;
    }

    /**
     * Sets a handler to manage exceptions thrown during task execution.
     * If all retries fail, this handler provides a fallback result.
     *
     * @param catchHandler a function that processes a Throwable and returns a fallback value of type T
     * @return this Try instance for fluent configuration
     */
    public Try<T> whenCatch(Function<Throwable, T> catchHandler) {
        this.catchHandler = catchHandler;
        return this;
    }

    /**
     * Configures the retry policy for task execution.
     * Defines how many times to retry and the delay between attempts.
     *
     * @param retryCount       number of retry attempts (minimum 1)
     * @param retryDelayMillis delay between retries in milliseconds (non-negative)
     * @return this Try instance for fluent configuration
     */
    public Try<T> retry(int retryCount, long retryDelayMillis) {
        this.retryCount = Math.max(retryCount, 1);
        this.retryDelayMillis = Math.max(retryDelayMillis, 0);
        return this;
    }

    /**
     * Applies the bulkhead pattern to limit concurrent task execution.
     * Configures a thread pool with a maximum number of threads, queue size, and wait time.
     *
     * @param poolId               unique identifier for the thread pool (enables pool sharing across instances)
     * @param maxConcurrentThreads maximum number of concurrent threads
     * @param maxQueueSize         maximum number of tasks allowed	In the queue
     * @param maxWaitMillis        maximum wait time for task submission in milliseconds
     * @return this Try instance for fluent configuration
     */
    public Try<T> bulkhead(String poolId, int maxConcurrentThreads, int maxQueueSize, long maxWaitMillis) {
        this.poolId = poolId;
        this.maxConcurrentThreads = maxConcurrentThreads;
        this.maxQueueSize = maxQueueSize;
        this.maxWaitMillis = maxWaitMillis;

        if (Objects.nonNull(poolId) && !poolId.isEmpty()) {
            this.executor = poolMap.computeIfAbsent(poolId, id -> createExecutor());
            this.semaphore = semaphoreMap.computeIfAbsent(poolId, id -> new Semaphore(maxConcurrentThreads));
        } else {
            this.executor = createExecutor();
            this.semaphore = new Semaphore(maxConcurrentThreads);
        }

        return this;
    }

    /**
     * Applies the bulkhead pattern using an existing thread pool identified by {@code poolId}.
     * If the pool and semaphore with the given {@code poolId} exist in the shared maps,
     * they are assigned to this Try instance to enforce concurrency limits.
     * Otherwise, a RuntimeException is thrown, guiding the user to create the pool
     * using {@link #bulkhead(String, int, int, long)}.
     *
     * @param poolId unique identifier of the thread pool to use
     * @return this Try instance for fluent configuration
     * @throws RuntimeException if the pool or semaphore for the given {@code poolId} does not exist
     */
    public Try<T> bulkhead(String poolId) {
        this.poolId = poolId;
        if (Objects.nonNull(poolMap.get(poolId)) && Objects.nonNull(semaphoreMap.get(poolId))) {
            this.executor = poolMap.get(poolId);
            this.semaphore = semaphoreMap.get(poolId);
        } else {
            throw new RuntimeException("Pool " + poolId + " not found, creating a new one using bulkhead(String poolId, int maxConcurrentThreads, int maxQueueSize, long maxWaitMillis)");
        }
        return this;
    }

    /**
     * Sets a time limit for task execution.
     * If the task exceeds this limit, it is canceled and a TimeoutExecutionException is thrown.
     *
     * @param timeLimitMillis maximum execution time in milliseconds
     * @return this Try instance for fluent configuration
     */
    public Try<T> timeLimit(long timeLimitMillis) {
        this.timeLimitMillis = timeLimitMillis;
        return this;
    }

    /**
     * Executes the task with the configured retry, bulkhead, and time limit policies.
     * Returns the task's result or the fallback result if retries are exhausted.
     *
     * @return the result of the task or fallback value
     * @throws RuntimeException if no catch handler is set and all retries fail
     */
    public T build() {
        int attempts = 0;
        if (Objects.isNull(poolId))
            bulkhead(DefaultPoolConfig.DEFAULT_POOL_ID, DefaultPoolConfig.MAX_CONCURRENT_THREADS, DefaultPoolConfig.MAX_QUEUE_SIZE, DefaultPoolConfig.MAX_QUEUE_WAIT_MILLIS);
        while (true) {
            try {
                attempts++;
                Callable<T> task = createTask();

                if (Objects.nonNull(executor)) {
                    return submitWithBulkhead(task);
                } else {
                    return task.call();
                }
            } catch (Throwable e) {
                if (attempts >= retryCount) {
                    return Objects.nonNull(catchHandler) ? catchHandler.apply(e) : throwRuntime(e);
                }
                sleep(retryDelayMillis);
            }
        }
    }

    /**
     * Creates a thread pool executor based on the configured concurrency and queue settings.
     *
     * @return a new ThreadPoolExecutor instance
     */
    private ThreadPoolExecutor createExecutor() {
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(Math.max(maxQueueSize, 1));
        return new ThreadPoolExecutor(maxConcurrentThreads, maxConcurrentThreads, 0L, TimeUnit.MILLISECONDS, queue, new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * Creates a callable task that executes the block, optionally with a time limit.
     * If a time limit is set, the task runs in the thread pool and is monitored for timeout.
     *
     * @return a Callable representing the task
     */
    private Callable<T> createTask() {
        return () -> {
            if (timeLimitMillis > 0) {
                Future<T> future = executor.submit(block::body);
                try {
                    return future.get(timeLimitMillis, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    throw new TimeoutExecutionException("Time limit exceeded", e);
                }
            } else {
                return block.body();
            }
        };
    }

    /**
     * Submits a task to the thread pool while respecting bulkhead constraints.
     * Ensures the task adheres to concurrency limits and queue capacity.
     *
     * @param task the task to execute
     * @return the result of the task
     * @throws Exception if the task fails, the queue is full, or the wait time is exceeded
     */
    private T submitWithBulkhead(Callable<T> task) throws Exception {
        // ------------------ Acquire Semaphore ------------------
        if (!semaphore.tryAcquire(maxWaitMillis, TimeUnit.MILLISECONDS)) {
            throw new BulkheadRejectedExecutionException("Bulkhead queue wait timeout");
        }

        RunnableFuture<T> futureTask = new FutureTask<>(task);

        try {
            // ------------------ Submit with queue capacity check ------------------
            if (!executor.getQueue().offer(futureTask, maxWaitMillis, TimeUnit.MILLISECONDS)) {
                throw new BulkheadRejectedExecutionException("Bulkhead queue capacity exceeded");
            }
            executor.execute(futureTask);

            return futureTask.get();
        } catch (ExecutionException e) {
            throw (e.getCause() instanceof Exception) ? (Exception) e.getCause() : e;
        } finally {
            semaphore.release();
        }
    }

    /**
     * Pauses the current thread for a specified duration.
     * Used to introduce a delay between retry attempts.
     *
     * @param millis the duration to pause in milliseconds
     */
    private void sleep(long millis) {
        try {
            if (millis > 0) Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Wraps a throwable in a RuntimeException and throws it.
     * Used when retries are exhausted and no catch handler is provided.
     *
     * @param e the throwable to wrap
     * @return never returns; always throws a RuntimeException
     * @throws RuntimeException wrapping the provided throwable
     */
    private T throwRuntime(Throwable e) {
        throw new RuntimeException(e);
    }
}