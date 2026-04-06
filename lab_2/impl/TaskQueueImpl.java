package lab_2.impl;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lab_2.api.Task;
import lab_2.api.TaskQueue;

public class TaskQueueImpl implements TaskQueue {

    private final Queue<Task> tasks = new LinkedList<>();

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private long totalDuration = 0;

    @Override
    public void add(Task task) {
        rwLock.writeLock().lock();
        try {
            tasks.add(task);
            totalDuration += task.getDurationSeconds();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public Task pop() {
        rwLock.writeLock().lock();
        try {
            Task task = tasks.poll();
            if (task != null) {
                totalDuration -= task.getDurationSeconds();
            }
            return task;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public long getTotalDuration() {
        rwLock.readLock().lock();
        try {
            return totalDuration;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        rwLock.writeLock().lock();
        try {
            tasks.clear();
            totalDuration = 0;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

}
