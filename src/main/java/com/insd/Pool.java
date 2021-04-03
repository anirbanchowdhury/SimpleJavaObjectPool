package com.insd;

public interface Pool<Poolable> {
    boolean initializePool()throws InstantiationException;
    Poolable takeFromPool() throws InstantiationException;
    void returnToPool(Poolable t);
    int size();
}
