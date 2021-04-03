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
* -XX:+FlightRecorder -XX:StartFlightRecording=duration=120s,filename=nopool-small.jfr -Xms512M -Xmx1024M -XX:+UseG1GC
* false true nopool-small 0
*
*
* -XX:+FlightRecorder -XX:StartFlightRecording=duration=120s,filename=pool-small.jfr -Xms512M -Xmx1024M -XX:+UseG1GC
* true true pool-small 100
*
*
* -XX:+FlightRecorder -XX:StartFlightRecording=duration=120s,filename=nopool-large.jfr -Xms512M -Xmx1024M -XX:+UseG1GC
* false false nopool-large 100
*
* */

@Slf4j
public class Entry {

    private String csvFilename;
    private Histogram histogram;

    BasicPool pool;

    public Entry(String fname,boolean usePool, boolean typeOfObject , int initialSize){
        log.error("fname = {}. usePool = {}, typeOfObject = {}, initialSize = {}", fname,usePool,typeOfObject,initialSize);
        csvFilename = fname+".csv";
        histogram = new Histogram(36000000L,3);
        try{
        pool = typeOfObject ? new BasicPool (SmallObject.class,usePool,initialSize) : new BasicPool (LargeObject.class,usePool,initialSize);
        if(!pool.initializePool()){
            throw new RuntimeException("Bad pool init");
        }
        } catch (InstantiationException e) {
            log.error("Error,{}",e.toString());
        }
    }
    public void start(){

        try(BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get(csvFilename), StandardOpenOption.CREATE);
        ){
            long countOfObjects = 0;
            Poolable obj = null;
            bufferedWriter.write("Count, min, max, 50,90,99,99.99");
            bufferedWriter.newLine();

            //TODO : PARAMETERIZE
            final int RUNTIME_LENGTH = 1;

            long currentTime = System.nanoTime();
            long endTime = currentTime + TimeUnit.MINUTES.toNanos(RUNTIME_LENGTH);

            long startMilis = System.currentTimeMillis();
            long changeTime = 0;

            while(currentTime < endTime){
                obj = (Poolable) pool.takeFromPool();

                if(obj == null){
                    log.info("Pool size = {}",pool.size());
                    throw new NullPointerException("Bad pool object");
                }
                pool.returnToPool(obj);
                histogram.recordValue(System.nanoTime() - currentTime);
                countOfObjects += 1;
                changeTime = checkAndWriteStats(bufferedWriter,currentTime,changeTime);
                currentTime = System.nanoTime(); // reset again

            }
            logFinishingMessage(countOfObjects, startMilis);
            bufferedWriter.flush();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private final void logFinishingMessage(long counter, long startMilis){
        long endMilis = System.currentTimeMillis();
        long timeInMinutes = TimeUnit.MILLISECONDS.toMinutes(endMilis -startMilis);
        log.warn("Throughput = {} over {} minute ", counter/timeInMinutes, timeInMinutes);

    }

    private final long checkAndWriteStats(BufferedWriter writer, long currentTime, long changeTime) throws IOException {
        if(changeTime != currentTime/1_000_000_000){
            changeTime = System.nanoTime()/1_000_000_000 ;
            log.info(getStats(histogram));
            writer.write(getStatsAsCSV(histogram));
            writer.newLine();
            histogram.reset();
        }
        return changeTime;
    }


    public final String getStats(Histogram histogram){
        StringBuilder builder = new StringBuilder("[")
                .append("]count=").append(histogram.getTotalCount())
                .append(";Min=").append(histogram.getTotalCount())
                .append(";Max=").append(histogram.getTotalCount())
                .append(";50th percentile =").append(histogram.getValueAtPercentile(50))
                .append(";90th percentile =").append(histogram.getValueAtPercentile(90))
                .append(";99th percentile =").append(histogram.getValueAtPercentile(99))
                .append(";99.99th percentile =").append(histogram.getValueAtPercentile(99.99));
        return builder.toString();
    }

    public final String getStatsAsCSV( Histogram histogram){
        StringBuilder builder = new StringBuilder("[")

                .append("]").append(histogram.getTotalCount())
                .append(",").append(histogram.getTotalCount())
                .append(",").append(histogram.getTotalCount())
                .append(",").append(histogram.getValueAtPercentile(50))
                .append(",").append(histogram.getValueAtPercentile(90))
                .append(",").append(histogram.getValueAtPercentile(99))
                .append(",").append(histogram.getValueAtPercentile(99.99));
        return builder.toString();
    }

    public static void main(String[] args) {
       /* //TODO : can parameterize
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
        log.info("Running on {} cores on {}",Runtime.getRuntime().availableProcessors(),System.getProperty("os.name"));
        /* Note below is NOT a good benchmark as the JVM is not restarting */
        new Entry("nopool-small",false,true,0).start();
        new Entry("pool-small",true,true,100).start();
        new Entry("nopool-large",false,false,0).start();
        new Entry("pool-large",true,false,100).start();
    }
}
