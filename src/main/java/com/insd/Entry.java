package com.insd;

import lombok.extern.slf4j.Slf4j;
import org.HdrHistogram.Histogram;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;


/*
 *
 * -XX:+UnlockCommercialFeatures - add if prior to JDK11
 * -XX:+FlightRecorder -XX:StartFlightRecording=duration=120s,filename=nopool-small.jfr -Xms512M -Xmx1024M -XX:+UseG1GC
 * false true nopool-small 0
 *
 * */

@Slf4j
public class Entry {

    private Pool pool;
    private String csvFilename;
    private Histogram histogram;

    public Entry(String fname,boolean usePool, boolean isSmallObject , int initialSize){
        log.error("fname = {}. usePool = {}, isSmallObject = {}, initialSize = {}", fname,usePool,isSmallObject,initialSize);
        csvFilename = new StringBuilder(fname).append(".csv").toString();
        histogram = new Histogram(36000000L,3);
        try{
            pool = isSmallObject ? new BasicPool (SmallObject.class,usePool,initialSize) : new BasicPool (LargeObject.class,usePool,initialSize);
            if(!pool.initializePool()){
                throw new RuntimeException("Couldn't instantiate pool ");
            }
        } catch (InstantiationException e) {
            log.error("Couldn't instantiate pool , error = {}",e.toString());
        }
    }

    public void start(){
        try(BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get(csvFilename), StandardOpenOption.CREATE)){
            long countOfObjects = 0;
            Poolable obj ;
            bufferedWriter.write("Count, min, max, 50th percentile,90th percentile,99th percentile,99.99th percentile"); // csv header
            bufferedWriter.newLine();

            long currentTime = System.nanoTime();
            //TODO : PARAMETERIZE if needed, this is the test run length in MINUTES
            final int RUNTIME_LENGTH_MINUTES = 1;
            long endTime = currentTime + TimeUnit.MINUTES.toNanos(RUNTIME_LENGTH_MINUTES);

            long startMilis = System.currentTimeMillis();
            long changeTime = 0;

            /*
                Basic test is take from pool, cleanup,  return to pool
            * */
            while(currentTime < endTime){
                obj = (Poolable) pool.takeFromPool();

                if(obj == null){
                    log.info("Pool size = {}",pool.size());
                    throw new NullPointerException("Bad pool object");
                }
                /* Actual Matching logic / publish obj to other ports */
                obj.cleanup();
                pool.returnToPool(obj);
                histogram.recordValue(System.nanoTime() - currentTime);
                countOfObjects += 1;
                changeTime = checkAndWriteStats(bufferedWriter,currentTime,changeTime);
                currentTime = System.nanoTime(); // reset again for the histogram to be accurate
            }
            log.info("Throughput = {} over {} minute ", countOfObjects/RUNTIME_LENGTH_MINUTES, RUNTIME_LENGTH_MINUTES);
            bufferedWriter.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private final long checkAndWriteStats(BufferedWriter writer, long currentTime, long changeTime) throws IOException {
        if(changeTime != currentTime/1_000_000_000){ //write every 1s
            changeTime = System.nanoTime()/1_000_000_000 ;
            log.info(getStats(histogram));
            writer.write(getStatsAsCSV(histogram));
            writer.newLine();
            histogram.reset();
        }
        return changeTime;
    }


    public final String getStats(Histogram histogram){
        StringBuilder builder = new StringBuilder("count=").append(histogram.getTotalCount())
                .append(";Min=").append(histogram.getMinValue())
                .append(";Max=").append(histogram.getMaxValue())
                .append(";50th percentile =").append(histogram.getValueAtPercentile(50))
                .append(";90th percentile =").append(histogram.getValueAtPercentile(90))
                .append(";99th percentile =").append(histogram.getValueAtPercentile(99))
                .append(";99.99th percentile =").append(histogram.getValueAtPercentile(99.99));
        return builder.toString();
    }

    public final String getStatsAsCSV( Histogram histogram){
        StringBuilder builder = new StringBuilder("")
                .append(histogram.getTotalCount())
                .append(",").append(histogram.getMinValue())
                .append(",").append(histogram.getMaxValue())
                .append(",").append(histogram.getValueAtPercentile(50))
                .append(",").append(histogram.getValueAtPercentile(90))
                .append(",").append(histogram.getValueAtPercentile(99))
                .append(",").append(histogram.getValueAtPercentile(99.99));
        return builder.toString();
    }

    public static void main(String[] args) {
       /* TODO : can parameterize if reqd.
        boolean usePool = false;
        boolean smallObjects = true;
        String csvFileName = null;
        int initialSize = 0;
        if(args.length == 4) {
            usePool = Boolean.parseBoolean(args[0]);
            smallObjects = Boolean.parseBoolean(args[1]);
            csvFileName = args[2];
            initialSize = Integer.parseInt(args[3]);
        }*/
        log.info("Running on {} cores on {} OS on {} JDK ",Runtime.getRuntime().availableProcessors(),System.getProperty("os.name"),System.getProperty("java.version"));
        /* WARNING : Do NOT run below lines at the same time one after another. It's NOT a good benchmark as the JVM is not restarting in between */
        //new Entry("nopool-small",false,true,0).start();
        //new Entry("pool-small",true,true,100).start();
        //new Entry("nopool-large",false,false,0).start();
        new Entry("pool-large",true,false,100).start();
    }
}
