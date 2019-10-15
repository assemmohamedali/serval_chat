package org.servalproject.mid.networking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import org.servalproject.mid.Serval;
import org.servalproject.servalchat.BuildConfig;
import org.servalproject.servalchat.R;
import org.servalproject.servaldna.ServalDFailureException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by jeremy on 2/11/16.
 */
public class Hotspot extends NetworkInfo {

	private static final String TAG = "Hotspot";
	private static final String profileName = "saved_hotspot";

	WifiConfiguration saved;
	WifiConfiguration servalConfiguration;
	WifiConfiguration current;
	boolean restoring = false;
	private String lastError = null;

	private final WifiManager mgr;

	private static Method getWifiApState;
	private static Method isWifiApEnabled;
	private static Method setWifiApEnabled;
	private static Method getWifiApConfiguration;

	protected Hotspot(Serval serval) {
		super(serval);
		mgr = (WifiManager) serval.context.getSystemService(Context.WIFI_SERVICE);
		servalConfiguration = new WifiConfiguration();
		servalConfiguration.SSID = serval.context.getString(R.string.SSID);
		servalConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

		testUserConfig();
		if (saved == null)
			saved = readProfile(profileName);

		// Every time setState called, Observer being updated to notify for changing (onUpdate in ObserverSet)
		setState(getWifiApState());
	}

	public boolean isServalConfig(){
		return isEqual(current, servalConfiguration);
	}

	private void testUserConfig(){
		try {
			WifiConfiguration current = getWifiApConfiguration();
			if (current != null && !isEqual(current, servalConfiguration)) {
				saveProfile(profileName, current);
				saved = current;
			}
			this.current = current;
		}catch (SecurityException e){
			Log.e(TAG, e.getMessage(), e);
		}
	}

	public static Hotspot getHotspot(Serval serval){
		Class<?> cls=WifiManager.class;
		for (Method method:cls.getDeclaredMethods()){
			String methodName=method.getName();
			if (methodName.equals("getWifiApState")){
				getWifiApState=method;
			}else if (methodName.equals("isWifiApEnabled")){
				isWifiApEnabled=method;
			}else if (methodName.equals("setWifiApEnabled")){
				setWifiApEnabled=method;
			}else if (methodName.equals("getWifiApConfiguration")){
				getWifiApConfiguration=method;
			}
		}

		if (getWifiApState == null || isWifiApEnabled == null
					|| setWifiApEnabled == null || getWifiApConfiguration == null)
			return null;

		return new Hotspot(serval);
	}

	private void testCause(InvocationTargetException e){
		Throwable cause = e.getCause();
		if (cause!=null) {
			lastError = cause.getMessage();
			if (cause instanceof RuntimeException)
				throw (RuntimeException) cause;
			throw new IllegalStateException(cause);
		}
	}

	public WifiConfiguration getWifiApConfiguration(){
		try {
			lastError = null;
			return (WifiConfiguration) getWifiApConfiguration.invoke(mgr);
		} catch (IllegalAccessException e) {
			// shouldn't happen
			lastError = e.getMessage();
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			testCause(e);
			lastError = e.getMessage();
			throw new IllegalStateException(e);
		}
	}

	public boolean isWifiApEnabled(){
		try {
			lastError = null;
			return (Boolean) isWifiApEnabled.invoke(mgr);
		} catch (IllegalAccessException e) {
			// shouldn't happen
			lastError = e.getMessage();
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			testCause(e);
			lastError = e.getMessage();
			throw new IllegalStateException(e);
		}
	}

	public static State statusToState(int state) {
		// Android's internal state constants were changed some time before
		// version 4.0
		if (state >= 10)
			state -= 10;

		switch (state) {
			case WifiManager.WIFI_STATE_DISABLED:
				return State.Off;
			case WifiManager.WIFI_STATE_DISABLING:
				return State.Stopping;
			case WifiManager.WIFI_STATE_ENABLED:
				return State.On;
			case WifiManager.WIFI_STATE_ENABLING:
				return State.Starting;
			default:
				Log.v(TAG, "Unhandled state "+state);
				return State.Error;
		}
	}

	public State getWifiApState(){
		try {
			lastError = null;
			return statusToState((Integer) getWifiApState.invoke(mgr));
		} catch (IllegalAccessException e) {
			// shouldn't happen
			lastError = e.getMessage();
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			testCause(e);
			lastError = e.getMessage();
			throw new IllegalStateException(e);
		}
	}

	public static boolean isEqual(WifiConfiguration config1, WifiConfiguration config2){
		if (config1 == config2)
			return true;
		if (config1 == null || config2 == null)
			return false;
		return config1.SSID.equals(config2.SSID)
				&& getKeyType(config1) == getKeyType(config2);
	}

	public boolean setWifiApEnabled(WifiConfiguration config, boolean enabled){
		if (config!=null)
			testUserConfig();
		try {
			lastError = null;
			return (Boolean) setWifiApEnabled.invoke(mgr, config, enabled);
		} catch (IllegalAccessException e) {
			// shouldn't happen
			lastError = e.getMessage();
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			testCause(e);
			lastError = e.getMessage();
			throw new IllegalStateException(e);
		}
	}

	private WifiConfiguration readProfile(String name) {
		SharedPreferences prefs = serval.context.getSharedPreferences(name, 0);
		String SSID = prefs.getString("ssid", null);
		if (SSID == null)
			return null;

		// android's WifiApConfigStore.java only uses these three fields.
		WifiConfiguration newConfig = new WifiConfiguration();
		newConfig.SSID = SSID;
		int keyType = prefs.getInt("key_type", WifiConfiguration.KeyMgmt.NONE);
		for (int i = 0; i <8; i++) {
			if ((keyType & (1<<i))!=0)
				newConfig.allowedKeyManagement.set(i);
		}
		newConfig.preSharedKey = prefs.getString("key", null);
		return newConfig;
	}

	private static int getKeyType(WifiConfiguration config){
		int keyType = 0;
		for (int i = 0; i <8; i++) {
			if (config.allowedKeyManagement.get(i))
				keyType |= (1<<i);
		}
		return keyType;
	}

	private void saveProfile(String name, WifiConfiguration config) {
		SharedPreferences prefs = serval.context.getSharedPreferences(name, 0);
		SharedPreferences.Editor ed = prefs.edit();
		ed.putString("ssid", config.SSID);
		ed.putInt("key_type", getKeyType(config));
		ed.putString("key", config.preSharedKey);
		ed.apply();
	}

	// This function called from WifiHotspotChanges class if state changed
	void onStateChanged(Intent intent) {
		testUserConfig();
		setState(statusToState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)));
		// New in Android O...
		//int errorCode = intent.getIntExtra("wifi_ap_error_code", WifiManager.HOTSPOT_NO_ERROR);
		//boolean localOnly = intent.getIntExtra("wifi_ap_mode", -1) == 2;
		String ifaceName = intent.getStringExtra("wifi_ap_interface_name");
		if (ifaceName!=null){
			try {
				serval.setInterface(Serval.HOTSPOT_INTERFACE, ifaceName);
			} catch (ServalDFailureException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	@Override
	public String getName(Context context) {
		return context.getString(R.string.hotspot);
	}

	@Override
	public String getStatus(Context context) {
		if (lastError != null)
			return lastError;
		if (restoring)
			return context.getString(R.string.restore_hotspot);

		switch (getState()){
			case Off:
				Networks.WifiGoal goal = Networks.getInstance().getGoal();
				if (goal == Networks.WifiGoal.HotspotOn || goal == Networks.WifiGoal.HotspotOnServalConfig)
					return context.getString(R.string.queued);
				break;

			case On:
				return context.getString(
						(getKeyType(current) & ~1) == 0 ?
						R.string.hotspot_open : R.string.hotspot_closed,
						current.SSID);
		}

		return super.getStatus(context);
	}

	// Get called from UI to open Hotspot
	@Override
	public void enable(Context context) {
		if (Build.VERSION.SDK_INT >= 23){
			if (!Settings.System.canWrite(context)){
				Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
						Uri.parse("package:" + BuildConfig.APPLICATION_ID));
				context.startActivity(i);
				return;
			}
		}
		boolean useConfig = serval.settings.getBoolean("hotspot_serval_config", true);
		Networks.getInstance().setWifiGoal(useConfig ? Networks.WifiGoal.HotspotOnServalConfig : Networks.WifiGoal.HotspotOn);
	}

	// Get called from UI to close Hotspot
	@Override
	public void disable(Context context) {
		if (Build.VERSION.SDK_INT >= 23) {
			if (!Settings.System.canWrite(context))
				return;
		}
		Networks.getInstance().setWifiGoal(Networks.WifiGoal.Off);
	}

	@Override
	public Intent getIntent(Context context) {
		PackageManager packageManager = context.getPackageManager();

		Intent i = new Intent();
		// Android 4(-ish)
		i.setClassName("com.android.settings", "com.android.settings.TetherSettings");
		ResolveInfo r = packageManager.resolveActivity(i, 0);
		if (r!=null){
			i.setClassName(r.activityInfo.packageName, r.activityInfo.name);
			return i;
		}
		// HTC roms
		i.setClassName("com.htc.WifiRouter", "com.htc.WifiRouter.WifiRouter");
		r = packageManager.resolveActivity(i, 0);
		if (r!=null){
			i.setClassName(r.activityInfo.packageName, r.activityInfo.name);
			return i;
		}
		// AOSP v2(-ish)
		i.setClassName("com.android.settings", "com.android.settings.wifi.WifiApSettings");
		r = packageManager.resolveActivity(i, 0);
		if (r!=null){
			i.setClassName(r.activityInfo.packageName, r.activityInfo.name);
			return i;
		}
		return null;
	}

	@Override
	public String getRadioName() {
		return null;
	}

	@Override
	public boolean isUsable(){
		return isOn() && isServalConfig();
	}
}
