# Abstract
Java’s strength as a developer friendly, language managed memory is also its primary obstacle to ultra low latency applications (5-20 mics ingress to outgress).
Java’s default memory management and GC techniques are often the reason for unacceptable pause times. 
Among the many techniques used, one key technique used is object pooling. 
Benchmark test done below studies the impact of object pooling in 2 commodity grade machines - OSX and Windows8.1 using standard Oracle JDK 11.0.10 (LTS) and graalvm-ce-java11-21.0.0.2. The test covers two flavors of objects - small and large objects resembling a single ticker and basket of tickers respectively. 

# Caveats 
1. Below code is not thread-safe, but can be made so easily by changing the data structure to ArrayBlockingQueue and keeping a secondary data structure for additional capacity. 
2. Benchmark results were captured after a single run after JVM shutdown for each flavor, smallobject-no pool, smallobject-pool, largeobject-nopool and largeobject-pool respectively 
3. Averages of the 50th, 90th, 99th, 99.99th percentile were taken over a performance test done in 1 minute, with samples taken every second. The first 2 entries were discarded to avoid the usual JVM warmup related outliers. For production grade systems, we need to do more test runs to prove our hypothesis. 
4. FlightRecorder was also run during the benchmark to verify runtime profiles.

# Benchmark Summary 

1. Throughput (objects created/fetched per minute) was 1.3-2x times more in object pooled examples 
2. Latency differences of the highest magnitude came from the 99.99th percentile, in many cases order of magnitude lesser latency was observed in the object pooled example
3. Interestingly, graalvm performed better than Oracle’s JDK in the 90th percentile bucket for both small and large objects, but performed worse in the throughput department in both cases. Behavior was reproducible in 3 runs.

# Results 

https://github.com/anirbanchowdhury/SimpleJavaObjectPool/blob/master/Object%20Pool%20performance.docx 
