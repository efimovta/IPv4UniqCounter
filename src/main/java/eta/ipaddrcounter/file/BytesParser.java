package eta.ipaddrcounter.file;

public interface BytesParser {
    void parseBuffer(FastByteBuffer fastBuf);
    void afterLastBuffer();
}
