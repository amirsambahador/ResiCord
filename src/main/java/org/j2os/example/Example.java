package org.j2os.example;

import org.j2os.resicord.Block;
import org.j2os.resicord.Try;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Mohammad Ghaderi, Ali Ghaderi and Amirsam Bahador in 2025.
 * Version: 1.0.0
 */
@RestController
public class Example {
    @GetMapping("/type-0")
    public String type0() {
        return new Try<>(() -> {
            System.out.println("start :" + System.currentTimeMillis());
            if (true) throw new Exception("Force Exception");
            System.out.println("end :" + System.currentTimeMillis());
            return "type-0";
        }).whenCatch(e -> "Error: " + e.getMessage())
                .retry(3, 1000).build();
    }

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


}
