package com.dar.nclientv3.utility.network;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;

import com.dar.nclientv3.utility.LogUtility;

public class NetworkUtil {
    private volatile static ConnectionType type = ConnectionType.WIFI;

    public static ConnectionType getType() {
        return type;
    }

    public static void setType(ConnectionType x) {
        LogUtility.d("new Status: " + x);
        type = x;
    }

    private static ConnectionType getConnectivityPreLollipop(ConnectivityManager cm) {
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null) return ConnectionType.WIFI;
        if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
            return ConnectionType.CELLULAR;

        return ConnectionType.WIFI;
    }

    private static ConnectionType getConnectivityPostLollipop(ConnectivityManager cm, Network network) {
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        if (capabilities == null) {
            return ConnectionType.WIFI;
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            return ConnectionType.CELLULAR;
        return ConnectionType.WIFI;
    }

    public static void initConnectivity(@NonNull Context context) {
        context = context.getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;

        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        builder.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET);
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                setType(getConnectivityPostLollipop(cm, network));
            }
        };
        try {
            cm.registerNetworkCallback(builder.build(), callback);
            Network[] networks = cm.getAllNetworks();
            if (networks.length > 0)
                setType(getConnectivityPostLollipop(cm, networks[0]));
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public enum ConnectionType {NO_CONNECTION, WIFI, CELLULAR}
}
