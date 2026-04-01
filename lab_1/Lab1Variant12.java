import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;

public class Lab1Variant12 {

    public static void main(String[] args) throws InterruptedException {
        int logicalCores = Runtime.getRuntime().availableProcessors();
        int physicalCores = logicalCores; // Для Apple M1

        System.out.println("=== Характеристики системи ===");
        System.out.println("ОС: " + System.getProperty("os.name"));
        System.out.println("Архітектура: " + System.getProperty("os.arch"));
        System.out.println("Логічні ядра: " + logicalCores);
        System.out.println("Фізичні ядра: " + physicalCores);
        System.out.println("========================================\n");

        int[] matrixSizes = {5000, 10000, 15000, 20000};
        int[] threadCounts = {1, 2, 4, 8, 16, 32, 64, 128}; 

        int iterations = 10;

        try {
            Files.createDirectories(Paths.get("results"));
            try (PrintWriter writer = new PrintWriter(new FileWriter("results/res.csv"))) {
                writer.println("Size,Threads,TimeMs");

                for (int size : matrixSizes) {
                    System.out.println("Обробка матриці " + size + "x" + size);
                    for (int threads : threadCounts) {
                        long totalDuration = 0;

                        for (int i = 0; i < iterations; i++) {
                            int[][] matrix = generateMatrix(size);
                            long startTime = System.nanoTime();

                            if (threads == 1) {
                                solveSequential(matrix);
                            } else {
                                solveParallel(matrix, threads);
                            }

                            long endTime = System.nanoTime();
                            totalDuration += (endTime - startTime);
                        }

                        long averageDurationMs = (totalDuration / iterations) / 1_000_000;
                        writer.printf("%d,%d,%d%n", size, threads, averageDurationMs);
                        System.out.printf("  > Потоків: %-3d | Сер. час (%d іт.): %d ms%n", 
                                          threads, iterations, averageDurationMs);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Помилка: " + e.getMessage());
        }
    }

    private static int[][] generateMatrix(int n) {
        int[][] matrix = new int[n][n];
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = rand.nextInt(1, 10);
            }
        }
        return matrix;
    }

    private static void solveSequential(int[][] matrix) {
        int n = matrix.length;
        for (int col = 0; col < n; col++) {
            int product = 1;
            for (int row = 0; row < n; row++) {
                product *= matrix[row][col];
            }
            matrix[n - 1 - col][col] = product;
        }
    }

    private static void solveParallel(int[][] matrix, int threadCount) throws InterruptedException {
        int n = matrix.length;
        Thread[] threads = new Thread[threadCount];
        int chunkSize = (n + threadCount - 1) / threadCount;

        for (int t = 0; t < threadCount; t++) {
            final int startCol = t * chunkSize;
            final int endCol = Math.min(startCol + chunkSize, n);
            threads[t] = new Thread(() -> {
                for (int col = startCol; col < endCol; col++) {
                    int product = 1;
                    for (int row = 0; row < n; row++) {
                        product *= matrix[row][col];
                    }
                    matrix[n - 1 - col][col] = product;
                }
            });
            threads[t].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }
}
