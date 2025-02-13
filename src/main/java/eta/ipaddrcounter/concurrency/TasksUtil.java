package eta.ipaddrcounter.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class TasksUtil {

    private TasksUtil() {
    }

    public static void executeAndWait(List<? extends Runnable> tasks, int threadsNumber, final String threadPrefix) {
        ThreadFactory threadFactory = getThreadFactoryForExecutor(threadPrefix);
        try (ExecutorService executor = Executors.newFixedThreadPool(threadsNumber, threadFactory)) {
            List<Future<?>> futures = execute(tasks, executor);
            waitForFutures(futures, executor);
        }
    }

    public static ThreadFactory getThreadFactoryForExecutor(String threadPrefix) {
        return new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, threadPrefix + count.getAndIncrement());
            }
        };
    }

    public static List<Future<?>> execute(List<? extends Runnable> tasks, ExecutorService executor) {
        List<Future<?>> futures = new ArrayList<>();

        for (Runnable task : tasks) {
            Future<?> future = executor.submit(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    executor.shutdownNow();
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        return futures;
    }

    public static void waitForFutures(List<Future<?>> futures, ExecutorService executor) {
        try {
            for (Future<?> future : futures) {
                future.get();
            }
            executor.shutdown();
            while (!executor.awaitTermination(20, TimeUnit.SECONDS)) {
            // System.out.println("processing...");
            }
        } catch (ExecutionException e) {
            executor.shutdownNow();
            throw new RuntimeException("Task failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            throw new RuntimeException("Task interrupted", e);
        }
    }
}
