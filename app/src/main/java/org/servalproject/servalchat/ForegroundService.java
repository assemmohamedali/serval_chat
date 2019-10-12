package org.servalproject.servalchat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;

/**
 * Created by jeremy on 19/10/16.
 * Create a notification so that android wont just kill our process while a network is viable
 */
public class ForegroundService extends Service {

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && intent.getBooleanExtra("foreground", false)) {
			Intent navIntent = MainActivity.getIntentFor(this, Navigation.Networking, null, null, null);
			PendingIntent pending = PendingIntent.getActivity(this, 0, navIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			NotificationCompat.Builder notificationBuilder = null;

			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				NotificationChannel channel = new NotificationChannel("org.servalproject.servalchat", "Test Service", NotificationManager.IMPORTANCE_DEFAULT);
				channel.setLightColor(Color.BLUE);
				NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				notificationManager.createNotificationChannel(channel);
				notificationBuilder = new NotificationCompat.Builder(this, "org.servalproject.servalchat");
			} else {
				notificationBuilder = new NotificationCompat.Builder(this);
			}


			notificationBuilder.setSmallIcon(R.mipmap.serval_head);
			notificationBuilder.setContentTitle(getString(R.string.foreground_title));
			notificationBuilder.setContentText(getString(R.string.foreground_text));
			notificationBuilder.setContentIntent(pending);

			Notification notification = notificationBuilder.build();


			this.startForeground(-1, notification);
			return START_STICKY;
		} else {
			stopForeground(true);
			stopSelf();
			return START_NOT_STICKY;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
