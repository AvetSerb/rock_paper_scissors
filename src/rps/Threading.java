package rps;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class Threading {
    private Threading() {}

    public static ExecutorService clientExecutor(ThreadFactory fallbackFactory) {
        ExecutorService vt = tryVirtualThreadPerTaskExecutor();
        if (vt != null) return vt;
        return Executors.newCachedThreadPool(fallbackFactory);
    }

    private static ExecutorService tryVirtualThreadPerTaskExecutor() {
        try {
            Class<?> executors = Class.forName("java.util.concurrent.Executors");
            Method m = executors.getMethod("newVirtualThreadPerTaskExecutor");
            Object res = m.invoke(null);
            return (ExecutorService) res;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
