package eta;

import eta.ipaddrcounter.IPv4UniqCounter;
import eta.ipaddrcounter.SimpleIPv4UniqCounter;
import eta.ipaddrcounter.task.FileChunkProcessor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        long totalStartTime = System.currentTimeMillis();

        Path path = Paths.get("C:\\tools\\ip_addresses");
        if (args.length > 0) {
            path = Path.of(args[0]);
        }
        System.out.println("start counting ips at file: " + path);



//        IPv4UniqCounter counter = new IOSeparateIPv4UniqCounter();
        IPv4UniqCounter counter = new SimpleIPv4UniqCounter();
        int uniqueCount = counter.countUniqIPv4AtFile(path);




        long totalDuration = System.currentTimeMillis() - totalStartTime;
        long minutes = totalDuration / 60000;
        long seconds = (totalDuration % 60000) / 1000;
        System.out.println("Unique IPv4 addresses: " + uniqueCount);
        System.out.println("File bytes: " + path.toFile().length());
        System.out.println("Total execution time: " + minutes + "min" + seconds + "sec");
    }

}