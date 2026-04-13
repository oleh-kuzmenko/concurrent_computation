package lab_3;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class Client {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8080;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        int n = 10;
        int threads = 4;
        int[][] matrix = generateMatrix(n);
        String serverIp = HOST + ":" + PORT;

        log("SYSTEM", "START", "Initializing client");
        log("SYSTEM", "MATRIX_GEN", "Generated Matrix:" + formatMatrix(matrix));

        try (Socket socket = new Socket(HOST, PORT);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            log(serverIp, "CONNECT", "Connected to server");

            out.writeByte(0x01);
            out.writeInt(n);
            out.writeInt(threads);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    out.writeInt(matrix[i][j]);
                }
            }
            out.flush();
            log(serverIp, "SEND_CONFIG", String.format("Sent Size: %dx%d, Threads: %d", n, n, threads));

            if (in.readByte() != 0x01) {
                return;
            }
            int jobId = in.readInt();
            log(serverIp, "RECV_CONFIG", "Received JobID: " + jobId);

            out.writeByte(0x02);
            out.writeInt(jobId);
            out.flush();
            log(serverIp, "SEND_START", "Requested start for JobID: " + jobId);

            if (in.readByte() != 0x02) {
                return;
            }

            while (true) {
                Thread.sleep(200);
                out.writeByte(0x03);
                out.writeInt(jobId);
                out.flush();

                if (in.readByte() == 0x03) {
                    byte status = in.readByte();
                    if (status == 1) {
                        for (int i = 0; i < n; i++) {
                            for (int j = 0; j < n; j++) {
                                matrix[i][j] = in.readInt();
                            }
                        }
                        log(serverIp, "RECV_STATUS", "Status: DONE. Received computed matrix:" + formatMatrix(matrix));
                        break;
                    } else if (status == 2) {
                        log(serverIp, "RECV_STATUS", "Status: ERROR.");
                        break;
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            log(serverIp, "ERROR", e.getMessage());
        }
    }

    private static void log(String target, String action, String meta) {
        System.out.printf("[%s] [%s] %s - %s%n", LocalDateTime.now().format(DTF), target, action, meta);
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

    private static String formatMatrix(int[][] matrix) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (int[] row : matrix) {
            for (int val : row) {
                sb.append(String.format("%8d", val));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
