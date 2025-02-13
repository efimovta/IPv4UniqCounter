package eta.ipaddrcounter.file;

public class FastByteBuffer {
    public final byte[] array;
    public final int capacity;
    public int length;

    public FastByteBuffer(byte[] array) {
        this.array = array;
        this.capacity = array.length;
        this.length = 0;
    }
}
