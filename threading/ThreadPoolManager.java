package threading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

public class ThreadPoolManager {

    private static ThreadPoolManager instance;
    private final ExecutorService pool;

    private ThreadPoolManager() {
        pool = Executors.newFixedThreadPool(4);
    }

    public static synchronized ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }

    public <T> Future<T> submit(Callable<T> task) {
        return pool.submit(task);
    }

    public void shutdown() {
        pool.shutdown();
    }
}