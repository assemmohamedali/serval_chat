package org.servalproject.mid.networking;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;

import com.broadcasts.annotations.BroadcastReceiverActions;

import org.servalproject.mid.networking.receivers.Session;

@BroadcastReceiverActions({
        BluetoothDevice.ACTION_NAME_CHANGED, BluetoothAdapter.ACTION_DISCOVERY_STARTED, BluetoothAdapter.ACTION_DISCOVERY_FINISHED,
        BluetoothAdapter.ACTION_STATE_CHANGED, BluetoothAdapter.ACTION_SCAN_MODE_CHANGED, "android.net.wifi.WIFI_AP_STATE_CHANGED",
        "android.net.wifi.WIFI_STATE_CHANGED", WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
})

public class GlobalReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Session.getGlobalReceiverCallBack(context, intent);
    }
}
