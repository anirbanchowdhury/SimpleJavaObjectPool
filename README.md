# SimpleJavaObjectPool
Simple Java object pool to test out effect of pooling on both small and large objects. 
Note , this is not threadsafe and the objects are not cleaned up before they are returned to the pool. 
The implementation uses a simple ArrayDeque where the objects are retrieved from the beginning, returned to the end.

