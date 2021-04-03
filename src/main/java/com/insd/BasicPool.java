package com.insd;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;

import static java.lang.String.format;

@Slf4j
public class BasicPool<Poolable> implements Pool<Poolable> {

    private Class<Poolable> clazz;
    private ArrayDeque<Poolable> pool;
    //TODO : get from config properties
    private boolean usePool;
    private int initialSize ;


    public BasicPool(Class<Poolable> t, boolean usePool, int intialSize){
        this.clazz = t;
        this.usePool = usePool;
        this.initialSize = intialSize;
        pool = new ArrayDeque<>(this.initialSize);
        log.error("Using pool = {}, initialSize = {}, for {}",usePool,intialSize,clazz);
    }

    public final boolean initializePool() throws InstantiationException {
        if(pool.size() > 0 ){
            throw new InstantiationException(format("Already initialized pool : {0}",pool.size()));
        }

        if(!usePool){
            return true;
        }
        return initialSize == addObjectsToPool(this.initialSize);

    }

    private int addObjectsToPool(int count) throws InstantiationException{
        int added = 0;
        log.info("Going to add {} objects to pool", count);
        for (added = 0; added<count;added++){
            try {
                if(!pool.add(clazz.getDeclaredConstructor().newInstance())){
                    break; // if addition doesn't work, break out of loop ? Can also throw exception here.
                }
            } catch (InstantiationException | IllegalAccessException |InvocationTargetException|NoSuchMethodException e) {
               throw new InstantiationException(e.toString());
            }
        }
        return added;
    }
    public Poolable takeFromPool() throws InstantiationException{
        if (usePool){
            return pool.pollFirst();
        }
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException |InvocationTargetException|NoSuchMethodException e) {
            throw new InstantiationException(e.toString());
        }
    }

    public void returnToPool(Poolable t) {
        if(usePool){
            //TODO : clean first
            pool.addLast(t);
        }
    }

    @Override
    public int size() {
        return this.pool.size();
    }

}
