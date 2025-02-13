package eta.ipaddrcounter.bench;

import eta.ipaddrcounter.TestFileGenerator;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@State(Scope.Benchmark)
public class FileCreationState {

    public Path testFile;

    @Setup(Level.Trial)
    public void setUp() throws IOException {
        // Create a temporary test file
        testFile = Files.createTempFile("jmh-test-", ".txt");

        long fileSizeMb = 1000;
        int uniqCount = 100_000;
        TestFileGenerator.generateTestFile(testFile, fileSizeMb, uniqCount);
    }

    @TearDown(Level.Trial)
    public void tearDown() throws IOException {
        if (testFile != null) {
            Files.deleteIfExists(testFile);
        }
    }
}