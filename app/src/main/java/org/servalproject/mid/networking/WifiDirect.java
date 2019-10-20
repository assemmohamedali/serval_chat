package org.servalproject.mid.networking;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import org.servalproject.mid.Serval;
import org.servalproject.servalchat.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WifiDirect extends NetworkInfo implements WifiP2pManager.GroupInfoListener, WifiP2pManager.ConnectionInfoListener {
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel channel;
    private static final String TAG = "WifiDirect";
    private final String DeviceName = "Serval";
    private WifiP2pGroup current;

    private static Method setDeviceName;

    public static WifiDirect getWifiDirect(Serval serval) {
        Class<?> cls=WifiP2pManager.class;
        try {
            Method m = cls.getDeclaredMethod("setDeviceName", WifiP2pManager.Channel.class, String.class, WifiP2pManager.ActionListener.class);
            String methodName = m.getName();
            if(methodName.equals("setDeviceName"))
                setDeviceName=m;

            if(setDeviceName == null)
                return null;

            return new WifiDirect(serval);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected WifiDirect(Serval serval) {
        super(serval);
        mManager = (WifiP2pManager) serval.context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = mManager.initialize(serval.context, serval.context.getMainLooper(), null);
        if(channel == null) {
            Log.v(TAG, "Failed to create a channel");
        }
    }

    @Override
    public String getName(Context context) {
        return "Wifi-Direct";
    }

    /*@Override
    public String getStatus(Context context) {
        if (getState()==State.Off && Networks.getInstance().getGoal() == Networks.WifiGoal.ClientOn)
            return context.getString(R.string.queued);
        return super.getStatus(context);
    }*/

    private void createGroup() {
        mManager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Group has been created");
            }

            @Override
            public void onFailure(int i) {
                Log.d(TAG, "Unable to create a group");
            }
        });
    }

    private void setAPName() {

        try {
            setDeviceName.setAccessible(true);
            setDeviceName.invoke(mManager, channel, DeviceName,  new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Device name has been set to : " + DeviceName);
                    createGroup();
                }
                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Wifi Direct name cannot be changed");
                }
            });
            setDeviceName.setAccessible(false);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void enable(Context context) {
        /*
            * Enable WIFI (Pre-request)
            * Create WIFI-Direct Group
            * Enable WIFI-Direct Access point
            * Set WIFI-Direct state to on
         */
        setAPName();

    }

    @Override
    public void disable(Context context) {
        mManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.v(TAG, "Group removed");
            }

            @Override
            public void onFailure(int i) {
                Log.v(TAG, "Unable to remove a group");
            }
        });
    }

    @Override
    public Intent getIntent(Context context) {
        return null;
    }

    @Override
    public String getRadioName() {
        return "wifi-p2p";
    }

    public void onStateChanged(Intent intent) {
        //setState(State.On);
        android.net.NetworkInfo networkInfo = (android.net.NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        setState(statusToState(networkInfo.getState()));

        if(networkInfo.isConnected())
            mManager.requestConnectionInfo(channel, this);

    }

    public static State statusToState(android.net.NetworkInfo.State state) {
        switch (state) {
            case DISCONNECTED:
            case UNKNOWN:
                return State.Off;
            case CONNECTED:
                return State.Starting;
            case CONNECTING:
                return State.Starting;
            case DISCONNECTING:
                return State.Stopping;
            default:
                Log.v(TAG, "Unknown state: "+state);
                return State.Error;
        }
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {

        current = wifiP2pGroup;
        Log.v(TAG, "On group info available " + wifiP2pGroup.getNetworkName() + " and password " + wifiP2pGroup.getPassphrase());
        setState(State.On);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        if(wifiP2pInfo.isGroupOwner) {
            mManager.requestGroupInfo(channel, this);
            //setState(State.On);
        }
    }

    @Override
    public String getStatus(Context context) {

        switch (getState()){
            case On:
                return context.getString(
                        R.string.wifidirect_open, current.getNetworkName() + " & " + current.getPassphrase());
        }

        return super.getStatus(context);
    }
}
