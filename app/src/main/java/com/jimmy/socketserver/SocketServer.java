package com.jimmy.socketserver;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer extends Service {
    private static final String TAG = "SocketServer";

    Map<String, Socket> clients;
    private boolean isRunningServer = false;
    private ExecutorService threadPool;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.clients = new HashMap<>();
        this.isRunningServer = true;
        this.threadPool = Executors.newFixedThreadPool(30);

        SocketServerThread serverThread = new SocketServerThread();
        threadPool.execute(serverThread);
    }

    private class SocketServerThread extends Thread {

        private ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(10086);
                while (isRunningServer) {
                    Socket client = serverSocket.accept();
                    String hostAddress = client.getInetAddress().getHostAddress();
                    clients.put(hostAddress, client);
                    SocketClientReadingThread readingThread = new SocketClientReadingThread(client);
                    threadPool.execute(readingThread);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SocketClientReadingThread extends Thread {

        private Socket socket;
        private DataInputStream dis;

        public SocketClientReadingThread(Socket socket) {
            try {
                this.socket = socket;
                this.dis = new DataInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (isRunningServer) {
                    Log.d(TAG, socket.getInetAddress().getHostAddress() + "=" + dis.readUTF());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunningServer = false;
    }

}
