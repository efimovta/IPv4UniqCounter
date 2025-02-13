package eta.ipaddrcounter.bench;

import eta.ipaddrcounter.IOSeparateIPv4UniqCounter;
import eta.ipaddrcounter.SimpleIPv4UniqCounter;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;


@BenchmarkMode({Mode.SingleShotTime})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class AllBenchmark {

    public static void main(String[] args) throws Exception {
//        String fileSuffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yy__HH.mm"));
//        String resultFilename = "C:\\eta-tmp\\bench\\eta_benchmarkV4_result" + fileSuffix + ".txt";
//        String outputFile = "C:\\eta-tmp\\bench\\eta_benchmarkV4_output" + fileSuffix + ".txt";
//        System.setOut(new PrintStream(new TeeOutputStream(System.out, new FileOutputStream(outputFile)), true));

        new Runner(new OptionsBuilder()
                .include(AllBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(0)
                .measurementIterations(1)
//                .jvmArgs(
//                        "-Xms2G",
//                        "-Xmx2G",
//                        "-XX:+AlwaysPreTouch",
//                        "-XX:+UnlockExperimentalVMOptions",
//                        "-XX:+UseEpsilonGC"
//                )
//                .shouldDoGC(true)
//                .result(resultFilename)
//                .resultFormat(ResultFormatType.TEXT)
                .build()
        ).run();
    }

//    @Param({"18", "17", "16"})
//    public int threads;

    @Benchmark
    public void runSimple(FileCreationState state) {
        Path testFile = state.testFile;
        new SimpleIPv4UniqCounter().countUniqIPv4AtFile(testFile);
    }

    @Benchmark
    public void runIOSeparate(FileCreationState state)  {
        Path testFile = state.testFile;
        new IOSeparateIPv4UniqCounter().countUniqIPv4AtFile(testFile);
    }

}
