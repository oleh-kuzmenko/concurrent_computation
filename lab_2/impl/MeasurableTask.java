package lab_2.impl;

import lab_2.api.Task;

public class MeasurableTask implements Task {

    private final Runnable action;
    private final int durationSeconds;

    private final long createdAt;
    private long startedAt;
    private long finishedAt;

    public MeasurableTask(Runnable action, int durationSeconds) {
        this.action = action;
        this.durationSeconds = durationSeconds;
        this.createdAt = System.currentTimeMillis();
    }

    @Override
    public void execute() {
        this.startedAt = System.currentTimeMillis();
        try {
            Thread.sleep(durationSeconds * 1000L);
            action.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        this.finishedAt = System.currentTimeMillis();
    }

    @Override
    public int getDurationSeconds() {
        return durationSeconds;
    }

    public long getWaitTime() {
        return startedAt - createdAt;
    }

    public long getExecutionTime() {
        return finishedAt - startedAt;
    }
}
