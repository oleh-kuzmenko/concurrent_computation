package lab_2.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lab_2.api.MultiQueueThreadPool;
import lab_2.api.Task;
import lab_2.api.TaskQueue;

public class MultiQueueThreadPoolImpl implements MultiQueueThreadPool {

    private final List<TaskQueue> taskQueues = new ArrayList<>();

    private final List<Thread> workers = new ArrayList<>();

    private final List<Condition> queueWaiters = new ArrayList<>();

    private final ReentrantReadWriteLock globalLock = new ReentrantReadWriteLock();

    private boolean initialized = false;
    private boolean terminated = false;

    @Override
    public void initialize(int queueCount, int workerPerQueue) {
        globalLock.writeLock().lock();
        try {
            if (initialized) {
                return;
            }

            for (int i = 0; i < queueCount; i++) {
                taskQueues.add(new TaskQueueImpl());
                queueWaiters.add(globalLock.writeLock().newCondition());
            }

            for (int i = 0; i < queueCount; i++) {
                final int queueIdx = i;
                for (int j = 0; j < workerPerQueue; j++) {
                    Thread worker = new Thread(() -> routine(queueIdx), "Worker-Q" + queueIdx + "-T" + j);
                    workers.add(worker);
                    worker.start();
                }
            }
            initialized = true;
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    private void routine(int queueIndex) {
        TaskQueue myQueue = taskQueues.get(queueIndex);
        Condition myWaiter = queueWaiters.get(queueIndex);

        while (true) {
            Task task = null;
            globalLock.writeLock().lock();
            try {
                while (!terminated && (task = myQueue.pop()) == null) {
                    myWaiter.awaitUninterruptibly();
                }
                if (terminated && task == null) {
                    return;
                }
            } finally {
                globalLock.writeLock().unlock();
            }

            if (task != null) {
                task.execute();
            }
        }
    }

    @Override
    public void addTask(Task task) {
        globalLock.writeLock().lock();
        try {
            if (!initialized || terminated)
                return;

            TaskQueue targetQueue = taskQueues.stream()
                    .min(Comparator.comparingLong(TaskQueue::getTotalDuration))
                    .orElseThrow(() -> new IllegalStateException("No task queues available"));

            targetQueue.add(task);

            int idx = taskQueues.indexOf(targetQueue);
            queueWaiters.get(idx).signal();
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    @Override
    public void terminate() {
        globalLock.writeLock().lock();
        try {
            terminated = true;
            for (Condition c : queueWaiters) {
                c.signalAll();
            }
        } finally {
            globalLock.writeLock().unlock();
        }

        for (Thread w : workers) {
            try {
                w.join();
            } catch (InterruptedException ignored) {
                System.out.println("Worker thread interrupted during termination");
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isWorking() {
        globalLock.readLock().lock();
        try {
            return initialized && !terminated;
        } finally {
            globalLock.readLock().unlock();
        }
    }

}
