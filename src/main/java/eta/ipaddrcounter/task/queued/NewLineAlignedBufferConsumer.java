package eta.ipaddrcounter.task.queued;

import eta.ipaddrcounter.file.BytesParser;
import eta.ipaddrcounter.file.FastByteBuffer;
import eta.ipaddrcounter.task.FileChunkProcessor;

import java.util.concurrent.BlockingQueue;


public class NewLineAlignedBufferConsumer implements Runnable {
    private final BlockingQueue<FastByteBuffer> freeBuffers;
    private final BlockingQueue<FastByteBuffer> workQueue;
    private final BytesParser parser;

    public NewLineAlignedBufferConsumer(BlockingQueue<FastByteBuffer> freeBuffers,
                                        BlockingQueue<FastByteBuffer> workQueue,
                                        BytesParser parser) {
        this.freeBuffers = freeBuffers;
        this.workQueue = workQueue;
        this.parser = parser;
    }

    @Override
    public void run() {
        try {
            while (true) {
                FastByteBuffer buffer = workQueue.take();
                if (buffer == NewLineAlignedBufferProducer.POISON_PILL) {
                    break;
                }
                parser.parseBuffer(buffer);
                buffer.length = 0;
                freeBuffers.put(buffer);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("thread was interrupted");
        }
        parser.afterLastBuffer();
    }
}
