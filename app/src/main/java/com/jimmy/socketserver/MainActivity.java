package com.jimmy.socketserver;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    SocketServer.SocketServerListener socketServerListener = new SocketServer.SocketServerListener() {
        @Override
        public void onNotifyClient(List<Socket> clients) {
            listAdapter.replaceDatas(clients);
        }

        @Override
        public void onNotifyNewMessage(String newMessage) {
            Toast.makeText(MainActivity.this, newMessage, Toast.LENGTH_LONG).show();
        }
    };
    private ListAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView listView = findViewById(R.id.client_list);
        listAdapter = new ListAdapter();
        listView.setAdapter(listAdapter);

        ServiceConnection conn = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                SocketServer socketServer = ((SocketServer.MyBinder) service).getSocketServer();
                socketServer.setSocketServerListener(socketServerListener);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        Intent intent = new Intent(this, SocketServer.class);
        bindService(intent, conn, BIND_AUTO_CREATE);
    }

    private class ListAdapter extends BaseAdapter {

        private List<Socket> datas = new ArrayList<>();

        @Override
        public int getCount() {
            return datas == null ? 0 : datas.size();
        }

        @Override
        public Socket getItem(int position) {
            return datas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.client_item_layout, parent, false);
                holder.text1 = convertView.findViewById(R.id.text1);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Socket socket = getItem(position);
            holder.text1.setText(socket.getInetAddress().getHostAddress());

            return convertView;
        }

        public void replaceDatas(List<Socket> datas) {
            this.datas = datas;
            notifyDataSetChanged();
        }

        private class ViewHolder {
            TextView text1;
        }
    }
}
