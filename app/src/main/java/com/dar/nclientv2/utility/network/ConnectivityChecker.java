package com.dar.nclientv2.utility.network;

import android.content.Context;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class ConnectivityChecker extends Thread {
    private final Context context;

    public ConnectivityChecker(Context context) {
        this.context = context;
    }

    @Override
    public void run() {

    }

    public boolean isOnline() {
        try {
            int timeoutMs = 1500;
            Socket sock = new Socket();
            SocketAddress sockaddr = new InetSocketAddress("8.8.8.8", 53);

            sock.connect(sockaddr, timeoutMs);
            sock.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public interface ConnectionResult {
        void isConnected();

        void isDisconnected();
    }
}
