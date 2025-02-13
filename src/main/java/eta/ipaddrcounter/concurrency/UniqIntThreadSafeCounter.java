package eta.ipaddrcounter.concurrency;

import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * A thread-safe counter for tracking unique integer values.
 * It may also be used for counting IPv4 addresses since they can be easily converted to an int.
 * <p>
 * This class uses an {@code AtomicIntegerArray} as a bit set, where each bit corresponds
 * to one possible integer value. The counter supports adding integers and counting the total number
 * of unique values.
 * <p>
 * The underlying array size is fixed at 2^27 integers, which is sufficient to represent all 2^32 possible
 * values (interpreted as unsigned 32-bit numbers).
 * </p>
 */
public class UniqIntThreadSafeCounter {
    private static final int INT_SIZE = 32;
    private static final int ARRAY_SIZE = 1 << 27; // 2^32 / 32
    private final AtomicIntegerArray bits = new AtomicIntegerArray(ARRAY_SIZE);

    /**
     * Adds an integer to the counter.
     * <p>
     * The value is treated as an unsigned 32-bit integer. This method uses bit-level operations
     * to mark the presence of the value. If the value has already been added, this method has no effect.
     *
     * @param value the integer value to add
     */
    public void add(int value) {
        long unsignedValue = value & 0xFFFFFFFFL;
        int index = (int) (unsignedValue / INT_SIZE);
        int mask = 1 << (unsignedValue % INT_SIZE);
        int prev, next;
        do {
            prev = bits.get(index);
            next = prev | mask;
            if (next == prev) return;
        } while (!bits.compareAndSet(index, prev, next));
    }

    /**
     * Returns the total number of unique integer values that have been added.
     *
     * @return the count of unique values.
     */
    public int getUniqCount() {
        int count = 0;
        for (int i = 0; i < bits.length(); i++) {
            count += Integer.bitCount(bits.get(i));
        }
        return count;
    }
}
