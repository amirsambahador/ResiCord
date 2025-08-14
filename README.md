<h1>Resi Cord</h1>

<p><strong>resicord</strong> is a high-level <strong>Java</strong> library for <strong>resilient task execution</strong> and <strong>concurrency management</strong>. Designed for <strong>microservice architectures</strong> and distributed systems, it provides a fluent API for managing <strong>Retries</strong>, <strong>Timeouts</strong>, <strong>Bulkhead isolation</strong>, and <strong>ThreadPool concurrency</strong>, ensuring <strong>stability and resilience</strong> under high load or failures.</p>

<h2>Features</h2>
<ul>
    <li><strong>Bulkhead Pattern:</strong> Prevent cascading failures by isolating tasks in separate thread pools.</li>
    <li><strong>Retry Mechanism:</strong> Retry failed tasks with configurable attempts and delays.</li>
    <li><strong>Timeout Handling:</strong> Limit task execution time with automatic cancellation and <code>TimeoutExecutionException</code>.</li>
    <li><strong>ThreadPool & Concurrency Control:</strong> Manage concurrency using <code>ThreadPoolExecutor</code> and <code>Semaphore</code>.</li>
    <li><strong>Resilience & Reliability:</strong> Combine Bulkhead, Retry, TimeLimit, and exception handling for stable execution.</li>
    <li><strong>Fluent API:</strong> Easy-to-read chaining for task configuration and execution.</li>
    <li><strong>Microservices Ready:</strong> Ideal for async tasks, service calls, event processing, and resource-limited operations.</li>
</ul>

<h2>Usage Example</h2>
<pre><code>String result = new Try&lt;&gt;(() -&gt; callRemoteService())
        .retry(3, 500)
        .bulkhead("service-pool", 10, 50, 1000)
        .timeLimit(2000)
        .whenCatch(e -&gt; "fallback")
        .build();
</code></pre>


<h3>1. <code>() -&gt; callRemoteService()</code></h3>
<ul>
<li>Represents the <strong>task</strong> to execute.</li>
<li><code>Try&lt;T&gt;</code> is generic; <code>T</code> is the type of the result.</li>
<li>Can be a <strong>lambda</strong> or a <code>Block&lt;T&gt;</code>.</li>
<li>In this example, <code>callRemoteService()</code> simulates a remote service call.</li>
</ul>

<h3>2. <code>.retry(3, 500)</code></h3>
<ul>
<li><strong>retryCount = 3</strong>: Number of retry attempts if the task fails.</li>
<li><strong>retryDelayMillis = 500</strong>: Delay between retries in milliseconds (0.5 sec).</li>
<li>Ensures task is retried up to 3 times with 500ms pause between attempts.</li>
</ul>

<h3>3. <code>.bulkhead("service-pool", 10, 50, 1000)</code></h3>
<p>Parameters in order:</p>
<ol>
<li><strong>poolId = "service-pool"</strong>: Identifier for the thread pool. Shared pools can be reused.</li>
<li><strong>maxConcurrentThreads = 10</strong>: Max threads allowed to execute concurrently.</li>
<li><strong>maxQueueSize = 50</strong>: Max tasks allowed in the waiting queue. Exceeding this triggers <code>BulkheadRejectedExecutionException</code>.</li>
<li><strong>maxWaitMillis = 1000</strong>: Maximum wait time for a free thread or queue slot. Exceeding this rejects the task.</li>
</ol>

<h3>4. <code>.timeLimit(2000)</code></h3>
<ul>
<li><strong>timeLimitMillis = 2000</strong>: Maximum allowed execution time in milliseconds.</li>
<li>If the task exceeds this, <code>TimeoutExecutionException</code> is thrown and the task is canceled.</li>
</ul>

<h3>5. <code>.whenCatch(e -&gt; "fallback")</code></h3>
<ul>
<li><strong>Exception handler</strong> for cases when retries are exhausted.</li>
<li><code>e</code> is the exception object.</li>
<li>Returns a fallback result, in this case <code>"fallback"</code>.</li>
</ul>

<h3>6. <code>.build()</code></h3>
<ul>
<li>Executes the task with all configurations applied.</li>
<li>Returns the task result or the fallback value.</li>
<li>Automatically handles retries, bulkhead limits, queue, and timeout.</li>
</ul>

<p><strong>Summary:</strong><br>
These parameters allow precise control over <strong>task retries, concurrency, queue limits, timeouts, and exception handling</strong>, providing resilience and stable execution without manual thread or exception management.
</p>

<h2>Exception Handling</h2>
<ul>
    <li><code>BulkheadRejectedExecutionException</code>: Thrown when a task cannot be executed due to concurrency or queue limits.</li>
    <li><code>TimeoutExecutionException</code>: Thrown when a task exceeds the specified time limit.</li>
</ul>

<h2>Default Configuration</h2>
<pre><code>String DEFAULT_POOL_ID = "Default-Pool-Id";
int MAX_CONCURRENT_THREADS = Integer.MAX_VALUE;
int MAX_QUEUE_SIZE = Integer.MAX_VALUE;
long MAX_QUEUE_WAIT_MILLIS = Long.MAX_VALUE;
</code></pre>

<h2>Spring Boot Example</h2>
<p>This example demonstrates how to integrate <code>resicord</code> with <strong>Spring Boot</strong> controllers, showing different types of task executions with retries, bulkhead isolation, and time limits.</p>

<h3>Example Controller</h3>
<pre><code>
  
package org.j2os.example;

import org.j2os.resicord.Block;
import org.j2os.resicord.Try;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Example {
  
    @GetMapping("/type-1")
    public Object type1() {
        var aTry = new Try<>(() -> {
            System.out.println("start :" + System.currentTimeMillis());
            Thread.sleep(5000);
            System.out.println("end :" + System.currentTimeMillis());
            return "type-1";
        });

        aTry.whenCatch(e -> "Error: " + e.getMessage())
                .retry(3, 1000)//3 retry, 1000 retryDelayMillis
                .bulkhead("type1-pool-id", 2, 4, 1000)
                .timeLimit(6000);

        return aTry.build();
    }

    @GetMapping("/type-2")
    public String type2() {
        return new Try<>(() -> {
            System.out.println("start :" + System.currentTimeMillis());
            Thread.sleep(5000);
            System.out.println("end :" + System.currentTimeMillis());
            return "type-2";
        }).whenCatch(e -> "Error: " + e.getMessage())
                .retry(3, 1000)
                .bulkhead("type2-pool-id", 2, 4, 1000)
                .timeLimit(6000).build();
    }

    @GetMapping("/type-3")
    public String type3() {
        return new Try<>(() -> {
            System.out.println("start :" + System.currentTimeMillis());
            Thread.sleep(5000);
            System.out.println("end :" + System.currentTimeMillis());
            return "type-3";
        }).whenCatch(e -> "Error: " + e.getMessage())
                .retry(3, 1000)
                .timeLimit(6000).build();
    }


    @GetMapping("/type-4")
    public String type4() {

        Block<String> tryBlock = () -> {
            System.out.println("start :" + System.currentTimeMillis());
            Thread.sleep(5000);
            System.out.println("end :" + System.currentTimeMillis());
            return "type-3";
        };

        return new Try<>(tryBlock)
                .whenCatch(e -> "Error: " + e.getMessage())
                .retry(3, 1000)
                .timeLimit(6000).build();
    }

    @GetMapping("/type-5")
    public String type5() {

        Block<String> tryBlock = () -> {
            System.out.println("start :" + System.currentTimeMillis());
            Thread.sleep(5000);
            System.out.println("end :" + System.currentTimeMillis());
            return "type-5";
        };

        return new Try<>(tryBlock)
                .whenCatch(e -> "Error: " + e.getMessage())
                .bulkhead("type2-pool-id")//shared pool
                .retry(3, 1000)
                .timeLimit(6000).build();
    }
  // Additional endpoints type-2 to type-5 demonstrate shared pools and time limits
}
</code></pre>

<h3>Explanation</h3>
<ul>
    <li><code>type1</code>: Demonstrates a task with <strong>bulkhead isolation</strong>, <strong>retry</strong>, and <strong>time limit</strong>.</li>
    <li><code>type2 - type5</code>: Examples of tasks using shared pools, time limits, and structured task execution.</li>
</ul>

<h2>Design Patterns & Best Practices</h2>
<ul>
    <li><strong>Functional Interface:</strong> <code>Block&lt;T&gt;</code> for defining tasks.</li>
    <li><strong>Concurrency Patterns:</strong> Using <code>ThreadPoolExecutor</code>, <code>Semaphore</code>, and <code>ConcurrentHashMap</code>.</li>
    <li><strong>Fluent API:</strong> Readable and maintainable code for task execution.</li>
    <li><strong>Resilient Design:</strong> Suitable for <strong>microservices</strong> and distributed systems.</li>
</ul>

<h2>Java2 Open Source Organization</h2>
<ul>
    <li><a href="https://www.j2os.org/eng/">http://www.j2os.org - English</a></li>
    <li><a href="https://www.j2os.org/">http://www.j2os.org - Persian</a></li>
</ul>
