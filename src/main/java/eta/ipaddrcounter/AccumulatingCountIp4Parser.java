package eta.ipaddrcounter;

import eta.ipaddrcounter.concurrency.UniqIntThreadSafeCounter;
import eta.ipaddrcounter.file.BytesParser;
import eta.ipaddrcounter.file.FastByteBuffer;
import eta.ipaddrcounter.task.FileChunkProcessor;

public class AccumulatingCountIp4Parser implements BytesParser {
    private final UniqIntThreadSafeCounter counter;

    private int ipAsInt = 0;
    private int currentPart = 0;
    private int partCount = 0;

    public AccumulatingCountIp4Parser(UniqIntThreadSafeCounter counter) {
        this.counter = counter;
    }

    @Override
    public void parseBuffer(FastByteBuffer fastBuf) {
        int ip = this.ipAsInt;
        int part = this.currentPart;
        int count = this.partCount;

        byte[] array = fastBuf.array;
        int end = fastBuf.length;
        for (int i = 0; i < end; i++) {
            byte b = array[i];
            if (b >= '0' && b <= '9') {
                part = part * 10 + (b - '0');
            } else if (b == '.') {
                ip = (ip << 8) | part;
                part = 0;
                count++;
            } else if (b == '\n') {
                if (count == 3) {
                    ip = (ip << 8) | part;
                    counter.add(ip);
                }
                ip = 0;
                part = 0;
                count = 0;
            }
//            else {
//                System.err.println("Unexpected symbol: " + (char) b + " (" + b + ")");
//            }
        }

        this.ipAsInt = ip;
        this.currentPart = part;
        this.partCount = count;
    }

    @Override
    public void afterLastBuffer() {
        if (partCount == 3) {
            ipAsInt = (ipAsInt << 8) | currentPart;
            counter.add(ipAsInt);
        }
        ipAsInt = 0;
        currentPart = 0;
        partCount = 0;
    }
}
