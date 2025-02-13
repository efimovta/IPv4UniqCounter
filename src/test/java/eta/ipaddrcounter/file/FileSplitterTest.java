package eta.ipaddrcounter.file;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
class FileSplitterTest {

    @TempDir
    Path tempDir;

    @Test
    void testChunksSumEqualsFileSizeAndContiguous() throws IOException {
        // Setup
        File file = tempDir.resolve("testFile.txt").toFile();
        String content = "Line1\nLine2\nLine3\nLine4\nLine5\n";
        Files.write(file.toPath(), content.getBytes());

        // Execution
        long fileSize = file.length();
        int targetChunks = 3;
        long minChunkSize = 1;
        List<FileChunk> chunks = FileSplitter.splitOnNewLineAlignedChunks(targetChunks, file, minChunkSize);

        // Assertion
        assertEquals(3, chunks.size(), "Chunks list size wrong");
        assertEquals(0, chunks.get(0).start(), "First chunk should start at 0");
        assertEquals(fileSize, chunks.get(chunks.size() - 1).end(), "Last chunk should end at file size");

        long totalLength = 0;
        for (int i = 0; i < chunks.size(); i++) {
            FileChunk chunk = chunks.get(i);
            totalLength += (chunk.end() - chunk.start());
            if (i < chunks.size() - 1) {
                assertEquals(chunks.get(i).end(), chunks.get(i + 1).start(), "Chunks are not contiguous");
            }
        }
        assertEquals(fileSize, totalLength, "Sum of chunk lengths should equal file size");
    }

    @Test
    void testEmptyFile() throws IOException {
        // Setup
        File file = tempDir.resolve("empty.txt").toFile();
        Files.write(file.toPath(), "".getBytes());

        // Execution
        int targetChunks = 3;
        long minChunkSize = 1;
        List<FileChunk> chunks = FileSplitter.splitOnNewLineAlignedChunks(targetChunks, file, minChunkSize);

        // Assertion
        assertEquals(1, chunks.size(), "Chunks list size wrong");
        for (FileChunk chunk : chunks) {
            assertEquals(0, chunk.start(), "Chunk start should be 0 for an empty file");
            assertEquals(0, chunk.end(), "Chunk end should be 0 for an empty file");
        }
    }

    @Test
    void testSingleChunk() throws IOException {
        // Setup
        File file = tempDir.resolve("singleChunk.txt").toFile();
        String content = "This is a test file\nwith multiple lines\n";
        Files.write(file.toPath(), content.getBytes());

        // Execution
        int targetChunks = 1;
        long minChunkSize = 1;
        List<FileChunk> chunks = FileSplitter.splitOnNewLineAlignedChunks(targetChunks, file, minChunkSize);

        // Assertion
        assertEquals(1, chunks.size(), "There should be exactly one chunk");
        FileChunk chunk = chunks.get(0);
        assertEquals(0, chunk.start(), "Chunk should start at 0");
        assertEquals(file.length(), chunk.end(), "Chunk should cover the entire file");
    }

    @Test
    void testChunksNewLineAlignment() throws IOException {
        // Setup
        File file = tempDir.resolve("newlineAligned.txt").toFile();
        String content = "A\nB\nC\nD\nE\n";
        Files.write(file.toPath(), content.getBytes());

        // Execution
        int targetChunks = 2;
        long minChunkSize = 1;
        List<FileChunk> chunks = FileSplitter.splitOnNewLineAlignedChunks(targetChunks, file, minChunkSize);

        // Assertion
        for (int i = 0; i < chunks.size() - 1; i++) {
            FileChunk chunk = chunks.get(i);
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                raf.seek(chunk.end() - 1);
                int ch = raf.read();
                assertEquals('\n', ch, "Chunk ending does not align with newline character");
            }
        }
    }

    @Test
    void testThreeByteFileWithHundredChunks() throws IOException {
        // Setup
        File file = tempDir.resolve("threeBytes.txt").toFile();
        byte[] content = { 'A', 'B', 'C' };
        Files.write(file.toPath(), content);

        // Execution
        int targetChunks = 100;
        long minChunkSize = 1;
        List<FileChunk> chunks = FileSplitter.splitOnNewLineAlignedChunks(targetChunks, file, minChunkSize);

        // Assertion
        assertEquals(1, chunks.size(), "Expected 1 chunk for a 3-byte file with no newline");
        FileChunk only = chunks.get(0);
        assertEquals(0, only.start(), "Chunk should start at 0");
        assertEquals(3, only.end(), "Chunk should have size 3");
    }
}
