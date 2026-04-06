package lab_2.api;

/**
 * Інтерфейс пулу потоків з внутрішнім балансуванням навантаження.
 */
public interface MultiQueueThreadPool {

    /**
     * Ініціалізує пул потоків, створюючи вказану кількість черг та робочих потоків.
     * @param queueCount кількість черг задач.
     * @param workerPerQueue кількість робочих потоків на кожну чергу.
     */
    void initialize(int queueCount, int workerPerQueue);

    /**
     * Додає задачу до системи. 
     * Алгоритм автоматично обирає чергу з найменшим показником getTotalDuration().
     * @param task задача, яку необхідно додати.
     */
    void addTask(Task task);

    /**
     * Зупиняє всі робочі потоки та завершує роботу пулу.
     */
    void terminate();

    /**
     * Перевіряє статус роботи пулу.
     * @return true, якщо пул активний.
     */
    boolean isWorking();
}
