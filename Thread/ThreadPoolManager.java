

public class ThreadPoolManager {
    private static final float IO_BLOCKING_FACTOR = 0.8F;

    private static final Map<Integer, String> LABEL_MAP = new ConcurrentHashMap<>();

    private final int mMaxPoolSize;

    private ExecutorService mExecutors;

    private ThreadPoolManager() {
        final int processors = Runtime.getRuntime().availableProcessors();
        mMaxPoolSize = (int) (processors / (1 - IO_BLOCKING_FACTOR));
    }

    /**
     * 在线程中执行
     *
     * @param runnable 要执行的runnable
     * @param label    任务对应的线程命名，格式：模块缩写+下划线+方法名/任务描述
     */
    public void execute(@NonNull Runnable runnable, String label) {
        ExecutorService executors = getExecutors();
        if (executors != null) {
            LABEL_MAP.put(runnable.hashCode(), label);
            executors.execute(runnable);
        } else {
            new Thread(runnable).start();
        }
    }

    private ExecutorService getExecutors() {
        if (mExecutors == null) {
            mExecutors = new CustomExecutorService(0, mMaxPoolSize);
        }
        return mExecutors;
    }

    public static ThreadPoolManager instance() {
        return InstanceHolder.INSTANCE;
    }

    private static class InstanceHolder {

        private final static ThreadPoolManager INSTANCE = new ThreadPoolManager();
    }

    static class CustomExecutorService extends ThreadPoolExecutor {

        static final ThreadLocal<String> sThreadName = new ThreadLocal<>();

        public CustomExecutorService(int corePoolSize, int maximumPoolSize) {
            super(corePoolSize, maximumPoolSize, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            final int key = r.hashCode();
            final String label = LABEL_MAP.remove(key);
            if (label != null && !label.isEmpty()) {
                sThreadName.set(t.getName());
                t.setName(label);
            }
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            final String name = sThreadName.get();
            if (name != null && !name.isEmpty()) {
                Thread.currentThread().setName(name);
            }
            sThreadName.remove();
        }
    }
}