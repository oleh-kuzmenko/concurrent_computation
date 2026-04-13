import socket
import struct
import random
import time
from datetime import datetime

HOST = "192.168.0.169"
PORT = 8080

def log(target, action, meta):
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{now}] [{target}] {action} - {meta}")

def generate_matrix(n):
    return [[random.randint(1, 9) for _ in range(n)] for _ in range(n)]

def format_matrix(matrix):
    res = "\n"
    for row in matrix:
        res += "".join([f"{val:>8}" for val in row]) + "\n"
    return res

def send_int(sock, val):
    sock.sendall(struct.pack('!i', val))

def send_byte(sock, val):
    sock.sendall(struct.pack('!B', val))

def recv_int(sock):
    data = sock.recv(4)
    if not data:
        raise ConnectionError("Connection closed")
    return struct.unpack('!i', data)[0]

def recv_byte(sock):
    data = sock.recv(1)
    if not data:
        raise ConnectionError("Connection closed")
    return struct.unpack('!B', data)[0]

def main():
    n = 10
    threads = 4
    matrix = generate_matrix(n)
    server_ip = f"{HOST}:{PORT}"

    log("SYSTEM", "START", "Initializing Python client")
    log("SYSTEM", "MATRIX_GEN", "Generated Matrix:" + format_matrix(matrix))

    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.connect((HOST, PORT))
            log(server_ip, "CONNECT", "Connected to server")

            send_byte(s, 0x01)
            send_int(s, n)
            send_int(s, threads)
            for row in matrix:
                for val in row:
                    send_int(s, val)
            
            log(server_ip, "SEND_CONFIG", f"Sent Size: {n}x{n}, Threads: {threads}")

            if recv_byte(s) != 0x01:
                return
            job_id = recv_int(s)
            log(server_ip, "RECV_CONFIG", f"Received JobID: {job_id}")

            send_byte(s, 0x02)
            send_int(s, job_id)
            log(server_ip, "SEND_START", f"Requested start for JobID: {job_id}")

            if recv_byte(s) != 0x02:
                return

            while True:
                time.sleep(0.2)
                send_byte(s, 0x03)
                send_int(s, job_id)

                if recv_byte(s) == 0x03:
                    status = recv_byte(s)
                    if status == 1:
                        for i in range(n):
                            for j in range(n):
                                matrix[i][j] = recv_int(s)
                        log(server_ip, "RECV_STATUS", "Status: DONE. Received computed matrix:" + format_matrix(matrix))
                        break
                    elif status == 2:
                        log(server_ip, "RECV_STATUS", "Status: ERROR.")
                        break

    except Exception as e:
        log(server_ip, "ERROR", str(e))

if __name__ == "__main__":
    main()
