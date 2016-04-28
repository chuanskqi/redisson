package org.redisson;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.redisson.client.RedisClient;

public abstract class BaseConcurrentTest extends BaseTest {

    protected void testMultiInstanceConcurrency(int iterations, final RedissonRunnable runnable) throws InterruptedException {
        System.out.println("Multi Instance Concurrent Job Interation: " + iterations);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

        final Map<Integer, RedissonClient> instances = new HashMap<Integer, RedissonClient>();
        for (int i = 0; i < iterations; i++) {
            instances.put(i, BaseTest.createInstance());
        }

        long watch = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            final int n = i;
            executor.execute(() -> runnable.run(instances.get(n)));
        }

        executor.shutdown();
        Assert.assertTrue(executor.awaitTermination(5, TimeUnit.MINUTES));

        System.out.println("multi: " + (System.currentTimeMillis() - watch));

        executor = Executors.newCachedThreadPool();

        for (final RedissonClient redisson : instances.values()) {
            executor.execute(() -> redisson.shutdown());
        }

        executor.shutdown();
        Assert.assertTrue(executor.awaitTermination(5, TimeUnit.MINUTES));
    }

    protected void testSingleInstanceConcurrency(int iterations, final RedissonRunnable runnable) throws InterruptedException {
        System.out.println("Single Instance Concurrent Job Interation: " + iterations);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

        final RedissonClient redisson = BaseTest.createInstance();
        long watch = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    runnable.run(redisson);
                }
            });
        }

        executor.shutdown();
        Assert.assertTrue(executor.awaitTermination(5, TimeUnit.MINUTES));

        System.out.println(System.currentTimeMillis() - watch);

        redisson.shutdown();
    }

}
