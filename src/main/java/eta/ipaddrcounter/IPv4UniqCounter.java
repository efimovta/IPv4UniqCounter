package eta.ipaddrcounter;

import java.nio.file.Path;

public interface IPv4UniqCounter {
    /**
     * Counts the unique integer values in the given file.
     *
     * @param path the path to the input file
     * @return the total count of unique values found in the file
     * @throws IllegalArgumentException if the file is not accessible or parameters are invalid
     */
    int countUniqIPv4AtFile(Path path);
}
