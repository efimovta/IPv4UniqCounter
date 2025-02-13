package eta.ipaddrcounter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class IntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    public void testSimpleCounter() throws Exception {
        Path tempFile = tempDir.resolve("generated_ips.txt");
        
        long fileSizeMb = 100;  // 1GB
        int uniqCount = 100_000;
        TestFileGenerator.generateTestFile(tempFile, fileSizeMb, uniqCount);
        
        int result = new SimpleIPv4UniqCounter().countUniqIPv4AtFile(tempFile);
        
        assertEquals(uniqCount, result, "The counter should detect 100,000 unique IP addresses");
    }

    @Test
    public void testWithIOSeparation() throws Exception {
        Path tempFile = tempDir.resolve("generated_ips.txt");

        long fileSizeMb = 100;  // 1GB
        int uniqCount = 100_000;
        TestFileGenerator.generateTestFile(tempFile, fileSizeMb, uniqCount);

        int result = new IOSeparateIPv4UniqCounter().countUniqIPv4AtFile(tempFile);

        assertEquals(uniqCount, result, "The counter should detect 100,000 unique IP addresses");
    }
}
