package eta.ipaddrcounter.concurrency;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class UniqIntThreadSafeCounterTest {

    @Test
    public void testSingleThreadAdd() {
        UniqIntThreadSafeCounter counter = new UniqIntThreadSafeCounter();
        counter.add(1);
        counter.add(2);
        counter.add(3);
        counter.add(3);
        counter.add(2);
        counter.add(1);
        assertEquals(3, counter.getUniqCount(), "Expected 3 unique values");
    }

    @Disabled
    @Test
    public void testConcurrentAddSameValues() throws InterruptedException, ExecutionException {
        UniqIntThreadSafeCounter counter = new UniqIntThreadSafeCounter();
        int numThreads = 10;
        int[] values = { 100, 200, 300, 400, 500 };

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        Callable<Void> task = () -> {
            for (int value : values) {
                counter.add(value);
            }
            return null;
        };

        Future<?>[] futures = new Future<?>[numThreads];
        for (int i = 0; i < numThreads; i++) {
            futures[i] = executor.submit(task);
        }
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Even if multiple threads add the same set of values concurrently, they should be counted only once.
        assertEquals(values.length, counter.getUniqCount(), "Expected unique count to equal the number of distinct values");
    }

    @Disabled
    @Test
    public void testConcurrentAddDistinctValues() throws InterruptedException, ExecutionException {
        UniqIntThreadSafeCounter counter = new UniqIntThreadSafeCounter();
        int numThreads = 10;
        int valuesPerThread = 100;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        Future<?>[] futures = new Future<?>[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            futures[i] = executor.submit(() -> {
                int start = threadIndex * valuesPerThread;
                int end = start + valuesPerThread;
                for (int j = start; j < end; j++) {
                    counter.add(j);
                }
            });
        }
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        int expectedUniqueCount = numThreads * valuesPerThread;
        assertEquals(expectedUniqueCount, counter.getUniqCount(), "Expected unique count to equal the total number of distinct values added");
    }
}
