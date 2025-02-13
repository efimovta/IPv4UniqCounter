package eta.ipaddrcounter;

import eta.ipaddrcounter.concurrency.TasksUtil;
import eta.ipaddrcounter.concurrency.UniqIntThreadSafeCounter;
import eta.ipaddrcounter.file.FastByteBuffer;
import eta.ipaddrcounter.file.FileSplitter;
import eta.ipaddrcounter.task.queued.NewLineAlignedBufferConsumer;
import eta.ipaddrcounter.task.queued.NewLineAlignedBufferProducer;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * IOSeparateIPv4UniqCounter implements a two-stage pipeline for counting unique values in a file.
 * I/O threads read file chunks sequentially into a pool of free buffers, while CPU threads process the filled buffers.
 * All configuration settings (I/O threads, CPU threads, buffer size, and number of buffers) are provided via the constructor.
 */
public class IOSeparateIPv4UniqCounter implements IPv4UniqCounter {

    private static final int DEF_BUFFER_SIZE = 1024 * 1024;
    private final int ioThreads;
    private final int cpuThreads;
    private final int bufferSize;
    private final int buffersNumber;

    /**
     * Constructs an IOSeparateIPv4UniqCounter with the specified configuration.
     *
     * @param ioThreads     the number of I/O threads (e.g., 1 for HDD or cpuThreads/2 for SSD)
     * @param cpuThreads    the number of CPU processing threads
     * @param bufferSize    the size of each buffer in bytes
     * @param buffersNumber the total number of buffers (must be at least equal to cpuThreads)
     */
    public IOSeparateIPv4UniqCounter(int ioThreads, int cpuThreads, int bufferSize, int buffersNumber) {
        if (ioThreads <= 0) throw new IllegalArgumentException("IO threads must be greater than 0");
        if (cpuThreads <= 0) throw new IllegalArgumentException("CPU threads must be greater than 0");
        if (bufferSize <= 0) throw new IllegalArgumentException("Buffer size must be greater than 0");
        if (buffersNumber <= 0) throw new IllegalArgumentException("Buffers number must be greater than 0");
        if (buffersNumber < cpuThreads)
            throw new IllegalArgumentException("Buffers number must be at least equal to the number of CPU threads");
        this.ioThreads = ioThreads;
        this.cpuThreads = cpuThreads;
        this.bufferSize = bufferSize;
        this.buffersNumber = buffersNumber;
    }

    /**
     * Convenience constructor for SSD configurations.
     * Defaults:  cpuThreads = = available processors, buffer size = 1 MB,
     *  buffers number = cpuThreads * 3, I/O threads = cpuThreads/2.
     *
     */
    public IOSeparateIPv4UniqCounter() {
        this(Math.max(Runtime.getRuntime().availableProcessors() / 2, 1),
                Runtime.getRuntime().availableProcessors(),
                DEF_BUFFER_SIZE,
                Runtime.getRuntime().availableProcessors() * 3);
    }

    /**
     * Convenience constructor for SSD configurations.
     * Defaults: buffer size = 1 MB, buffers number = cpuThreads * 3, I/O threads = cpuThreads/2.
     *
     * @param cpuThreads the number of CPU processing threads
     */
    public IOSeparateIPv4UniqCounter(int cpuThreads) {
        this(cpuThreads / 2, cpuThreads, DEF_BUFFER_SIZE, cpuThreads * 3);
    }

    /**
     * Convenience constructor for HDD configurations.
     * Defaults: buffer size = 1 MB, buffers number = cpuThreads * 3, I/O threads = 1.
     *
     * @param cpuThreads the number of CPU processing threads
     * @param useHDD     a flag indicating HDD configuration (true for HDD)
     */
    public IOSeparateIPv4UniqCounter(int cpuThreads, boolean useHDD) {
        this(useHDD ? 1 : cpuThreads / 2, cpuThreads, DEF_BUFFER_SIZE, cpuThreads * 3);
    }

    /**
     * Counts the unique values in the given file using separate I/O and CPU processing.
     * I/O threads read file chunks into free buffers while CPU threads process the filled buffers.
     *
     * @param path the path to the input file
     * @return the total count of unique values found in the file
     * @throws IllegalArgumentException if the file is not accessible or parameters are invalid
     */
    @Override
    public int countUniqIPv4AtFile(Path path) {
        if (path == null) throw new IllegalArgumentException("Path must not be null");

        File file = path.toFile();
        if (!file.exists())
            throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
        if (!file.isFile())
            throw new IllegalArgumentException("Not a valid file: " + file.getAbsolutePath());
        if (!file.canRead())
            throw new IllegalArgumentException("File is not readable: " + file.getAbsolutePath());

        return countUniqAtFileInternal(file);
    }

    private int countUniqAtFileInternal(File file) {
        UniqIntThreadSafeCounter counter = new UniqIntThreadSafeCounter();

        BlockingQueue<FastByteBuffer> freeBuffers = new ArrayBlockingQueue<>(buffersNumber);
        BlockingQueue<FastByteBuffer> workQueue = new ArrayBlockingQueue<>(buffersNumber + cpuThreads);//+ for PILL

        for (int i = 0; i < buffersNumber; i++) {
            freeBuffers.add(new FastByteBuffer(new byte[bufferSize]));
        }

        AtomicLong fileReadersCounter = new AtomicLong(ioThreads);
        var chunks = FileSplitter.splitOnNewLineAlignedChunks(ioThreads, file, bufferSize);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No file chunks created");
        }

        var ioTasks = chunks.stream()
                .map(chunk -> new NewLineAlignedBufferProducer(
                        file,
                        chunk.start(),
                        chunk.end(),
                        freeBuffers,
                        workQueue,
                        fileReadersCounter,
                        cpuThreads,
                        bufferSize))
                .toList();

        var cpuTasks = new ArrayList<Runnable>(cpuThreads);
        for (int i = 0; i < cpuThreads; i++) {
            AccumulatingCountIp4Parser parser = new AccumulatingCountIp4Parser(counter);
            cpuTasks.add(new NewLineAlignedBufferConsumer(freeBuffers, workQueue, parser));
        }

        ThreadFactory ioThreadFactory = TasksUtil.getThreadFactoryForExecutor("io-ipaddrcounter");
        ThreadFactory cpuThreadFactory = TasksUtil.getThreadFactoryForExecutor("cpu-ipaddrcounter");
        try (ExecutorService ioExecutor = Executors.newFixedThreadPool(ioThreads, ioThreadFactory);
             ExecutorService cpuExecutor = Executors.newFixedThreadPool(cpuThreads, cpuThreadFactory)) {

            List<Future<?>> ioFutures = TasksUtil.execute(ioTasks, ioExecutor);
            List<Future<?>> cpuFutures = TasksUtil.execute(cpuTasks, cpuExecutor);

            TasksUtil.waitForFutures(ioFutures, ioExecutor);
            TasksUtil.waitForFutures(cpuFutures, cpuExecutor);
        }

        return counter.getUniqCount();
    }
}
