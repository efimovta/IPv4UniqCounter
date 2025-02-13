package eta.ipaddrcounter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility for generating a test file containing random IP addresses.
 * Each line contains an IP address (e.g., "145.67.23.4").
 * The file is filled with repeated blocks of unique addresses until the desired size (in MB) is reached.
 */
public class TestFileGenerator {

    /**
     * Generates a file at the given path with approximately the specified size (in megabytes)
     * containing {@code uniqCount} unique IP addresses.
     *
     * @param filePath  the path of the file to generate
     * @param fileSizeMb the approximate target size of the file in megabytes
     * @param uniqCount  the number of unique IP addresses to generate
     * @throws IOException if an I/O error occurs
     */
    public static void generateTestFile(Path filePath, long fileSizeMb, int uniqCount) throws IOException {
        try (FileChannel channel = FileChannel.open(filePath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {

            // Generate a set of unique IP addresses (each as a string ending with a newline)
            Set<String> uniqIps = Stream.generate(() -> {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                return r.nextInt(1, 256) + "." +
                       r.nextInt(0, 256) + "." +
                       r.nextInt(0, 256) + "." +
                       r.nextInt(0, 256) + "\n";
            }).distinct().limit(uniqCount).collect(Collectors.toSet());

            // Convert each IP string to bytes
            List<byte[]> ipsAsBytes = uniqIps.stream()
                    .map(s -> s.getBytes(StandardCharsets.UTF_8))
                    .collect(Collectors.toList());
            int approxBytes = ipsAsBytes.stream().mapToInt(bytes -> bytes.length).sum();

            ByteBuffer buffer = ByteBuffer.allocateDirect(approxBytes + 100);
            long bytesToWrite = fileSizeMb * 1024 * 1024L;
            long written = 0;

            // Write blocks of unique IP addresses repeatedly until reaching the target size
            while (written < (bytesToWrite - approxBytes)) {
                buffer.clear();
                for (byte[] ip : ipsAsBytes) {
                    buffer.put(ip);
                }
                written += buffer.position();
                buffer.flip();
                channel.write(buffer);
            }
            // Write one final block to ensure the file ends with complete lines
            buffer.clear();
            for (byte[] ip : ipsAsBytes) {
                buffer.put(ip);
            }
            buffer.flip();
            channel.write(buffer);
        }
    }
}
