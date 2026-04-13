package lab_3;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private static final int PORT = 8080;
    private static final ConcurrentHashMap<Integer, Job> jobs = new ConcurrentHashMap<>();
    private static final AtomicInteger jobIdCounter = new AtomicInteger(1);
    private static final ExecutorService taskExecutor = Executors.newCachedThreadPool();
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        log("SYSTEM", "START", "Server listening on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                Thread.ofVirtual().start(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
        }
    }

    private static void log(String ip, String action, String meta) {
        System.out.printf("[%s] [%s] %s - %s%n", LocalDateTime.now().format(DTF), ip, action, meta);
    }

    private static void handleClient(Socket socket) {
        String clientIp = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        log(clientIp, "CONNECT", "New client connected");

        try (socket;
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            while (!socket.isClosed()) {
                byte command = in.readByte();
                switch (command) {
                    case 0x01 -> handleConfig(clientIp, in, out);
                    case 0x02 -> handleStart(clientIp, in, out);
                    case 0x03 -> handleStatus(clientIp, in, out);
                    default -> out.writeByte(0xFF);
                }
            }
        } catch (IOException e) {
            log(clientIp, "DISCONNECT", "Connection closed");
        }
    }

    private static void handleConfig(String ip, DataInputStream in, DataOutputStream out) throws IOException {
        int n = in.readInt();
        int threads = in.readInt();
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = in.readInt();
            }
        }
        int jobId = jobIdCounter.getAndIncrement();
        jobs.put(jobId, new Job(matrix, threads));
        
        log(ip, "CMD_CONFIG", String.format("Assigned JobID: %d, Size: %dx%d, Threads: %d", jobId, n, n, threads));
        
        out.writeByte(0x01);
        out.writeInt(jobId);
        out.flush();
    }

    private static void handleStart(String ip, DataInputStream in, DataOutputStream out) throws IOException {
        int jobId = in.readInt();
        Job job = jobs.get(jobId);
        if (job != null) {
            job.status = 0;
            taskExecutor.submit(() -> processJob(jobId, job));
            log(ip, "CMD_START", "Started JobID: " + jobId);
            out.writeByte(0x02);
        } else {
            out.writeByte(0xFF);
        }
        out.flush();
    }

    private static void handleStatus(String ip, DataInputStream in, DataOutputStream out) throws IOException {
        int jobId = in.readInt();
        Job job = jobs.get(jobId);
        if (job != null) {
            out.writeByte(0x03);
            out.writeByte(job.status);
            if (job.status == 1) {
                int n = job.matrix.length;
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        out.writeInt(job.matrix[i][j]);
                    }
                }
                jobs.remove(jobId);
                log(ip, "CMD_STATUS", "Result sent for JobID: " + jobId);
            }
        } else {
            out.writeByte(0xFF);
        }
        out.flush();
    }

    private static void processJob(int jobId, Job job) {
        log("SYSTEM", "PROCESS", "Executing JobID: " + jobId);
        try {
            int n = job.matrix.length;
            int threadCount = job.threads;
            Thread[] threads = new Thread[threadCount];
            int chunkSize = (n + threadCount - 1) / threadCount;
            for (int t = 0; t < threadCount; t++) {
                final int startCol = t * chunkSize;
                final int endCol = Math.min(startCol + chunkSize, n);
                threads[t] = new Thread(() -> {
                    for (int col = startCol; col < endCol; col++) {
                        int product = 1;
                        for (int row = 0; row < n; row++) {
                            product *= job.matrix[row][col];
                        }
                        job.matrix[n - 1 - col][col] = product;
                    }
                });
                threads[t].start();
            }
            for (Thread thread : threads) thread.join();
            job.status = 1;
            log("SYSTEM", "DONE", "Completed JobID: " + jobId);
        } catch (Exception e) {
            job.status = 2;
            log("SYSTEM", "ERROR", "Failed JobID: " + jobId);
        }
    }

    private static class Job {
        int[][] matrix;
        int threads;
        volatile int status = -1;
        Job(int[][] matrix, int threads) {
            this.matrix = matrix;
            this.threads = threads;
        }
    }
}
