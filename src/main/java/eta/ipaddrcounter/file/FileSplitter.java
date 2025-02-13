package eta.ipaddrcounter.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public final class FileSplitter {

    private FileSplitter() {
    }

    public static List<FileChunk> splitOnNewLineAlignedChunks(int targetChunks, File file, long minChunkSize) {
        try {
            return getChunksInternal(targetChunks, file, minChunkSize);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<FileChunk> getChunksInternal(int targetChunks, File file, long minChunkSize) throws IOException {
        List<FileChunk> chunks = new ArrayList<>();
        long fileSize = file.length();
        if (fileSize == 0) {
            chunks.add(new FileChunk(0, 0));
            return chunks;
        }
        int actualChunks = getActualChunks(targetChunks, minChunkSize, fileSize);
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] buffer = new byte[1024];
            long approxChunkSize = fileSize / actualChunks;
            long start = 0;
            for (int i = 0; i < actualChunks - 1; i++) {
                long approxEnd = start + approxChunkSize;
                long adjustedEnd = findNextNewline(raf, approxEnd, buffer);
                chunks.add(new FileChunk(start, adjustedEnd));
                start = adjustedEnd;
                if (start >= fileSize) {
                    break;
                }
            }
            if (start < fileSize) {
                chunks.add(new FileChunk(start, fileSize));
            }
        }
        return chunks;
    }

    private static int getActualChunks(int targetChunks, long minChunkSize, long fileSize) {
        int actualChunks = targetChunks;
        if (fileSize < targetChunks * minChunkSize) {
            actualChunks = (int) Math.ceil((double) fileSize / minChunkSize);
        }
        return Math.max(actualChunks, 1);
    }

    private static long findNextNewline(RandomAccessFile raf, long pos, byte[] tempBuf) throws IOException {
        raf.seek(pos);
        long currentPos = pos;
        int bytesRead;
        while ((bytesRead = raf.read(tempBuf)) != -1) {
            for (int i = 0; i < bytesRead; i++) {
                currentPos++;
                if (tempBuf[i] == '\n') {
                    return currentPos;
                }
            }
        }
        return currentPos;
    }
}
