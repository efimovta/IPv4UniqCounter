package eta.ipaddrcounter.task;

import eta.ipaddrcounter.concurrency.ThreadWasInterrupted;
import eta.ipaddrcounter.file.BytesParser;
import eta.ipaddrcounter.file.FastByteBuffer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;

public class FileChunkProcessor implements Runnable {

    private final File file;
    private final long startOffset;
    private final long endOffset;
    private final BytesParser parser;
    private final int bufferSize;

    public FileChunkProcessor(File file, long startOffset, long endOffset,
                              BytesParser parser, int bufferSize) {
        this.file = file;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.parser = parser;
        this.bufferSize = bufferSize;
    }

    @Override
    public void run() {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(startOffset);
            byte[] buf = new byte[bufferSize];
            FastByteBuffer fastBuf = new FastByteBuffer(buf);
            long currentPos = startOffset;
            while (currentPos < endOffset) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new ThreadWasInterrupted();
                }
                
                int bytesToRead = (int) Math.min(bufferSize, endOffset - currentPos);
                int bytesRead = raf.read(buf, 0, bytesToRead);
                if (bytesRead == -1) {
                    break;
                }
                currentPos += bytesRead;
                fastBuf.length = bytesRead;
                parser.parseBuffer(fastBuf);
            }
            parser.afterLastBuffer();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
