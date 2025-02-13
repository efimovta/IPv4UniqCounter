# IPv4 Unique Address Counter

This project is a high-performance tool for counting unique IPv4 addresses from very large files (hundreds of gigabytes). The input file must be a plain text file with one IPv4 address per line.

**Core Idea:**  
Use an `AtomicIntegerArray` as a bitmap with CAS support to efficiently count unique integers (e.g., IPv4 addresses converted to int).

**Implementations:**

- **SimpleIPv4UniqCounter:**  
  Splits the file into newline-aligned chunks and assigns each chunk to a processing thread. This approach works well on SSDs where fast I/O minimizes overhead. The parser `AccumulatingCountIp4Parser` maintains state across buffers to handle IP addresses that span multiple buffers.

- **IOSeparateIPv4UniqCounter:**  
  Employs a two-stage pipeline with separate thread pools:
  - **I/O Stage (Producers):**  
    The file is pre-split into chunks at newline boundaries. I/O threads read these chunks sequentially into free buffers, ensuring each buffer ends with a newline (or the end-of-file). If a buffer ends mid-line, the remaining bytes ("spill") are preserved for the next read.
  - **CPU Stage (Consumers):**  
    CPU threads process the filled buffers from a work queue using a stateful parser to correctly combine IP addresses that span buffers.

**Storage Considerations:**

- **HDD:** Use a single I/O thread for sequential reading to minimize disk head seeks, and allocate more threads for CPU processing.
- **SSD:** I/O can be parallelized since random access is much faster.


## Some performance numbers

- File size: ~111gb
- Uniq ip count: 1 000 000 000
- Memory: ~570mb
- Time: 1 minute 11 seconds (+/- 11sec)
- CPU Intel(R) Core(TM) i9-14900HX Base speed:	2,20 GHz  Cores: 8 (Logical processors: 16)
- SSD SAMSUNG MZVL21T0HCLR-00BL2





# Build & Run
 
> Java 21 

Windows:
```bash
.\mvnw.cmd clean package
```
Linux:
```bash
./mvnw clean package
```
Run:
```bash
java -jar .\target\IPv4UniqCounter.jar "C:/eta-tmp/ip_addresses"
```