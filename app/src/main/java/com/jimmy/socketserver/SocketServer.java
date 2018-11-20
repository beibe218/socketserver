package com.jimmy.socketserver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.jimmy.socketserver.SocketServer.Status.JOIN;
import static com.jimmy.socketserver.SocketServer.Status.NEW_MESSAGE;

public class SocketServer extends Service {
    private static final String TAG = "SocketServer";
    //Channel ID 必须保证唯一
    private static final String CHANNEL_ID = "com.jimmy.socket.notification.channel";

    Map<String, Socket> clients;
    private boolean isRunningServer = false;
    private ExecutorService threadPool;
    private MyHandler myHandler = new MyHandler(this);
    private SocketServerListener listener;

    public class MyBinder extends Binder {
        public SocketServer getSocketServer() {
            return SocketServer.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setForegroundService();
        }

        this.clients = new HashMap<>();
        this.isRunningServer = true;
        this.threadPool = Executors.newFixedThreadPool(30);

        SocketServerThread serverThread = new SocketServerThread();
        threadPool.execute(serverThread);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void setForegroundService() {
        //设置通知的重要程度
        int importance = NotificationManager.IMPORTANCE_LOW;
        //构建通知渠道
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SocketServer", importance);
        channel.setDescription("SocketServer");

        //在创建的通知渠道上发送通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.ic_launcher) //设置通知图标
                .setContentTitle("SocketServer")//设置通知标题
                .setContentText("New message")//设置通知内容
                .setAutoCancel(true) //用户触摸时，自动关闭
                .setOngoing(true);//设置处于运行状态

        //向系统注册通知渠道，注册后不能改变重要性以及其他通知行为
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }

        //将服务置于启动状态 NOTIFICATION_ID指的是创建的通知的ID
        startForeground(7100, builder.build());
    }

    private class SocketServerThread extends Thread {

        private ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(7100);
                while (isRunningServer) {
                    Socket client = serverSocket.accept();
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
                    Message msg = Message.obtain();
                    String s = dis.readUTF();
                    String[] strings = s.split("&");
                    if ("join".equals(strings[0])) {
                        msg.what = JOIN;
                        clients.put(strings[1], socket);
                    } else if ("message".equals(strings[0])) {
                        msg.what = NEW_MESSAGE;
                        msg.obj = "from " + strings[1] + "，msg " + strings[2];
                        sendMessageToClient(strings[1], strings[2]);
                    }
                    myHandler.sendMessage(msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMessageToClient(String userName, String message) throws IOException {
        for (Map.Entry<String, Socket> e : clients.entrySet()) {
            if (!userName.equals(e.getKey())) {
                Socket client = e.getValue();
                DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                dos.writeUTF(message);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunningServer = false;
        stopForeground(true);
    }

    public void setSocketServerListener(SocketServerListener listener) {
        this.listener = listener;
    }

    public interface SocketServerListener {
        void onNotifyClient(List<Socket> clients);

        void onNotifyNewMessage(String newMessage);
    }

    private static class MyHandler extends Handler {

        private WeakReference<SocketServer> weakReference;

        public MyHandler(SocketServer socketServer) {
            this.weakReference = new WeakReference<>(socketServer);
        }

        @Override
        public void handleMessage(Message msg) {
            SocketServer socketServer = weakReference.get();
            if (socketServer != null) {
                if (JOIN == msg.what) {
                    socketServer.notifyClients();
                } else if (NEW_MESSAGE == msg.what) {
                    socketServer.notifyNewMessage((String) msg.obj);
                }
            }
        }
    }

    public void notifyClients() {
        if (listener == null)
            return;

        List<Socket> clientList = new ArrayList<>();
        for (Map.Entry<String, Socket> e : clients.entrySet()) {
            clientList.add(e.getValue());
        }

        listener.onNotifyClient(clientList);
    }

    public void notifyNewMessage(String newMessage) {
        if (listener == null)
            return;

        listener.onNotifyNewMessage(newMessage);
    }

    public static final class Status {
        public static final int JOIN = 0;
        public static final int NEW_MESSAGE = 1;
    }

}
