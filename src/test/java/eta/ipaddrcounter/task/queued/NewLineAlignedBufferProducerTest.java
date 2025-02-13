package eta.ipaddrcounter.task.queued;

import eta.ipaddrcounter.file.FastByteBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

public class NewLineAlignedBufferProducerTest {

    @TempDir
    Path tempDir;

    @Test
    public void testBuffersEndWithNewline() throws IOException, InterruptedException {
        // Create a temporary file with known content: 20 lines.
        File tempFile = tempDir.resolve("testfile.txt").toFile();
        StringBuilder contentBuilder = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            contentBuilder.append("This is line ").append(i).append("\n");
        }
        java.nio.file.Files.writeString(tempFile.toPath(), contentBuilder.toString(), StandardCharsets.UTF_8);

        // Use a small buffer size to force splitting into multiple buffers.
        int bufferSize = 50; // bytes

        // Create the freeBuffers and workQueue.
        // Ensure that the total data does not exceed the number of free buffers.
        int freeBufferCount = 10;
        BlockingQueue<FastByteBuffer> freeBuffers = new ArrayBlockingQueue<>(freeBufferCount);
        BlockingQueue<FastByteBuffer> workQueue = new ArrayBlockingQueue<>(freeBufferCount);

        // Populate freeBuffers.
        for (int i = 0; i < freeBufferCount; i++) {
            freeBuffers.put(new FastByteBuffer(new byte[bufferSize]));
        }

        // Only one producer.
        AtomicLong fileReadersCounter = new AtomicLong(1);
        int consumersNumber = 1;

        // Instantiate the producer with startOffset=0 and endOffset = file length.
        long fileLength = tempFile.length();
        new NewLineAlignedBufferProducer(
                tempFile,
                0,
                fileLength,
                freeBuffers,
                workQueue,
                fileReadersCounter,
                consumersNumber,
                bufferSize
        ).run();

        // Poll the workQueue until the POISON_PILL is received.
        while (true) {
            FastByteBuffer buffer = workQueue.take();
            if (buffer == NewLineAlignedBufferProducer.POISON_PILL) {
                break;
            }
            // Ensure that the buffer is not empty.
            assertTrue(buffer.length > 0, "Buffer should not be empty");
            // Verify that the last byte is a newline character.
            byte lastByte = buffer.array[buffer.length - 1];
            assertEquals('\n', lastByte, "Buffer must end with a newline character");
        }
    }
}
