package eta.ipaddrcounter;

import eta.ipaddrcounter.concurrency.TasksUtil;
import eta.ipaddrcounter.concurrency.UniqIntThreadSafeCounter;
import eta.ipaddrcounter.file.BytesParser;
import eta.ipaddrcounter.file.FileChunk;
import eta.ipaddrcounter.task.FileChunkProcessor;
import eta.ipaddrcounter.file.FileSplitter;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * SimpleIPv4UniqCounter implements a simple, single-stage approach for counting unique values in a file.
 * In this implementation, the file is split into newline-aligned chunks and processed in parallel using a single thread pool.
 * All settings (threads number and buffer size) are provided via the constructor.
 */
public class SimpleIPv4UniqCounter implements IPv4UniqCounter {

    private final int threadsNumber;
    private final int bufferSize;

    /**
     * Constructs a SimpleIPv4UniqCounter with the specified number of threads and buffer size.
     *
     * @param threadsNumber the number of processing threads to use
     * @param bufferSize    the size of each buffer in bytes
     */
    public SimpleIPv4UniqCounter(int threadsNumber, int bufferSize) {
        if (threadsNumber <= 0) throw new IllegalArgumentException("Threads number must be greater than 0");
        if (bufferSize <= 0) throw new IllegalArgumentException("Buffer size must be greater than 0");
        this.threadsNumber = threadsNumber;
        this.bufferSize = bufferSize;
    }

    /**
     * Constructs a SimpleIPv4UniqCounter with default settings:
     * threads number = available processors and buffer size = 1 MB.
     */
    public SimpleIPv4UniqCounter() {
        this(Runtime.getRuntime().availableProcessors(), 1024 * 1024);
    }

    /**
     * Constructs a SimpleIPv4UniqCounter with default settings:
     * buffer size = 1 MB.
     */
    public SimpleIPv4UniqCounter(int threadsNumber) {
        this(threadsNumber, 1024 * 1024);
    }

    /**
     * Counts the unique values in the given file by splitting it into newline-aligned chunks
     * and processing them in parallel using the configured settings.
     *
     * @param path the path to the input file
     * @return the total count of unique values found in the file
     * @throws IllegalArgumentException if the file is not accessible or parameters are invalid
     */
    @Override
    public int countUniqIPv4AtFile(Path path) {
        if (path == null) throw new NullPointerException("Path must not be null");

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

        List<FileChunk> chunks = FileSplitter.splitOnNewLineAlignedChunks(threadsNumber, file, bufferSize);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No file chunks created");
        }

        List<FileChunkProcessor> tasks = chunks.stream().map(chunk -> {
            BytesParser parser = new AccumulatingCountIp4Parser(counter);
            return new FileChunkProcessor(file, chunk.start(), chunk.end(), parser, bufferSize);
        }).toList();

        TasksUtil.executeAndWait(tasks, threadsNumber, "ipaddrcounter-");

        return counter.getUniqCount();
    }
}
