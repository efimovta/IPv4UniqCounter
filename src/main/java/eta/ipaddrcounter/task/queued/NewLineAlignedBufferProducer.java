package eta.ipaddrcounter.task.queued;

import eta.ipaddrcounter.file.FastByteBuffer;
import eta.ipaddrcounter.task.FileChunkProcessor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class NewLineAlignedBufferProducer implements Runnable {

    public static final FastByteBuffer POISON_PILL = new FastByteBuffer(new byte[0]);

    private final File file;
    private final long startOffset;
    private final long endOffset;
    private final BlockingQueue<FastByteBuffer> freeBuffers;
    private final BlockingQueue<FastByteBuffer> workQueue;
    private final AtomicLong fileReadersCounter;
    private final int bufferSize;
    private final int consumersNumber;

    public NewLineAlignedBufferProducer(File file, long startOffset, long endOffset,
                                        BlockingQueue<FastByteBuffer> freeBuffers,
                                        BlockingQueue<FastByteBuffer> workQueue,
                                        AtomicLong fileReadersCounter,
                                        int consumersNumber,
                                        int bufferSize) {
        this.file = file;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.freeBuffers = freeBuffers;
        this.workQueue = workQueue;
        this.fileReadersCounter = fileReadersCounter;
        this.consumersNumber = consumersNumber;
        this.bufferSize = bufferSize;
    }

    @Override
    public void run() {
        try {
            readLoop();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("thread was interrupted");
        }
    }

    private void readLoop() throws IOException, InterruptedException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(startOffset);
            byte[] leftover = new byte[bufferSize];
            int leftoverSize = 0;
            long currentPos = startOffset;
            while (currentPos < endOffset) {
                FastByteBuffer buffer = freeBuffers.take();
                buffer.length = 0;

                drainLeftoversToBuffer(leftoverSize, leftover, buffer);

                int maxToRead = getMaxToRead(currentPos, buffer);
                int bytesRead = raf.read(buffer.array, buffer.length, maxToRead);
                if (bytesRead == -1) {
                    break;
                }
                buffer.length += bytesRead;
                currentPos += bytesRead;

                leftoverSize = alignBufferEndToNewlineKeepingLeftover(buffer, leftover);

                if (buffer.length > 0) {
                    workQueue.put(buffer);
                } else {
                    freeBuffers.put(buffer);
                }
            }
            sendLeftoverToWorkQueue(leftoverSize, leftover);
            notifyFileReadFinishedToWorkQueue();
        }
    }

    private void notifyFileReadFinishedToWorkQueue() throws InterruptedException {
        if (fileReadersCounter.decrementAndGet() == 0) {
            for (int i = 0; i < consumersNumber; i++) {
                workQueue.put(POISON_PILL);
            }
        }
    }

    private void sendLeftoverToWorkQueue(int leftoverSize, byte[] leftover) throws InterruptedException {
        if (leftoverSize > 0) {
            FastByteBuffer leftoverBuf = freeBuffers.take();
            leftoverBuf.length = 0;
            System.arraycopy(leftover, 0, leftoverBuf.array, 0, leftoverSize);
            leftoverBuf.length = leftoverSize;
            if (leftoverBuf.array[leftoverBuf.length - 1] != '\n') {
                if (leftoverBuf.length < leftoverBuf.capacity) {
                    leftoverBuf.array[leftoverBuf.length++] = '\n';
                }
            }
            workQueue.put(leftoverBuf);
        }
    }

    private static int alignBufferEndToNewlineKeepingLeftover(FastByteBuffer buffer, byte[] leftover) {
        int leftoverSize;
        if (buffer.length > 0 && buffer.array[buffer.length - 1] != '\n') {
            int lastNewlinePos = -1;
            for (int i = buffer.length - 1; i >= 0; i--) {
                if (buffer.array[i] == '\n') {
                    lastNewlinePos = i;
                    break;
                }
            }
            if (lastNewlinePos != -1) {
                int validLength = lastNewlinePos + 1;
                leftoverSize = buffer.length - validLength;
                System.arraycopy(buffer.array, validLength, leftover, 0, leftoverSize);
                buffer.length = validLength;
            } else {
                leftoverSize = buffer.length;
                buffer.length = 0;
            }
        } else {
            leftoverSize = 0;
        }
        return leftoverSize;
    }

    private static void drainLeftoversToBuffer(int leftoverSize, byte[] leftover, FastByteBuffer buffer) {
        if (leftoverSize > 0) {
            System.arraycopy(leftover, 0, buffer.array, 0, leftoverSize);
            buffer.length = leftoverSize;
        }
    }

    private int getMaxToRead(long currentPos, FastByteBuffer buffer) {
        int maxToRead;
        long leftBytesInFileChunk = endOffset - currentPos;
        if (leftBytesInFileChunk >= Integer.MAX_VALUE) {
            maxToRead = buffer.capacity - buffer.length;
        } else {
            maxToRead = Math.min(buffer.capacity - buffer.length, (int) leftBytesInFileChunk);
        }
        return maxToRead;
    }

}
