package com.interestscsc.normalizer;

import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.concurrent.*;

/**
 * User: allight
 */
public class Main {
    /**
     * Количество параллельно запускаемых потоков.
     * Оптимальное значение параметра зависит от машины.
     */
    private static final int THREADS_NUM = 4;
    private static final String THREAD_NAME_PREFIX = "Thread_";
    private static final int EXECUTION_PAUSE_SEC = 5;
    private static final Logger logger = Logger.getLogger(NormalizerTread.class);

    public static void main(String[] args) {
        ScheduledExecutorService es = Executors.newScheduledThreadPool(THREADS_NUM);
        LinkedList<ScheduledFuture> futures = new LinkedList<>();
        for (int i = 0; i < THREADS_NUM; i++) {
            futures.add(es.schedule(new NormalizerTread(THREAD_NAME_PREFIX + i),
                    EXECUTION_PAUSE_SEC * i, TimeUnit.SECONDS));
        }
        es.shutdown();
        futures.forEach((future) -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                logger.error(sw.toString());
            }
        });
    }
}
