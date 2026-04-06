package lab_2;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import lab_2.api.MultiQueueThreadPool;
import lab_2.impl.MeasurableTask;
import lab_2.impl.MultiQueueThreadPoolImpl;

public class Main {

    private static final int QUEUE_COUNT = 3;
    private static final int WORKER_PER_QUEUE = 2;

    public static void main(String[] args) throws InterruptedException {
        log("Початок тестування багатопотокового пулу з різними режимами навантаження...");
        runTest(20000, 30000, "Low");
        runTest(8000, 12000, "Optimal");
        runTest(2000, 5000, "High");
    }

    private static void runTest(int minDelay, int maxDelay, String modeName) throws InterruptedException {
        MultiQueueThreadPool pool = new MultiQueueThreadPoolImpl();
        pool.initialize(QUEUE_COUNT, WORKER_PER_QUEUE);

        int producerCount = 3;
        int testDurationSeconds = 60;
        List<MeasurableTask> allTasks = new CopyOnWriteArrayList<>();

        log("\nЗапуск режиму: " + modeName + " (Затримка: " + minDelay / 1000 + "-" + maxDelay / 1000 + "с)");

        for (int i = 0; i < producerCount; i++) {
            Thread.ofVirtual().start(() -> {
                long endTime = System.currentTimeMillis() + (testDurationSeconds * 1000);
                while (System.currentTimeMillis() < endTime) {
                    int duration = ThreadLocalRandom.current().nextInt(5, 21);
                    MeasurableTask task = new MeasurableTask(() -> {
                        log("Thread " + Thread.currentThread().getName()+ ": Виконання задачі (тривалість: " + duration + " сек)");
                    }, duration);
                    pool.addTask(task);
                    allTasks.add(task);

                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(minDelay, maxDelay));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }

        Thread.sleep(testDurationSeconds * 1000);
        pool.terminate();

        printStatistics(allTasks, QUEUE_COUNT, WORKER_PER_QUEUE);
    }

    private static void printStatistics(List<MeasurableTask> tasks, int queueCount, int workerPerQueue) {
        long totalWaitTime = 0;
        long totalExecutionTime = 0;
        int completedTasks = 0;

        for (MeasurableTask task : tasks) {
            if (task.getExecutionTime() > 0) {
                totalWaitTime += task.getWaitTime();
                totalExecutionTime += task.getExecutionTime();
                completedTasks++;
            }
        }

        double avgWait = completedTasks > 0 ? (double) totalWaitTime / completedTasks / 1000.0 : 0;
        double avgExec = completedTasks > 0 ? (double) totalExecutionTime / completedTasks / 1000.0 : 0;

        double throughput = completedTasks / 60.0;
        double queueLength = throughput * avgWait;

        log("--- Статистика ---");
        log("Середній час очікування: %.2f сек".formatted(avgWait));
        log("Середній час виконання: %.2f сек".formatted(avgExec));
        log("Ефективність задач: %.2f%%"
                .formatted((totalExecutionTime / (double) (totalWaitTime + totalExecutionTime)) * 100));
        log("Продуктивність пулу: %.2f задач/сек".formatted(throughput));
        log("Середня довжина черги: %.2f задач".formatted(queueLength));
        log("-----------------------------------");
    }

    private static void log(String message) {
        System.out.println(message);
    }
}
