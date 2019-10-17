package org.servalproject.servalchat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Interface;
import org.servalproject.mid.KnownPeers;
import org.servalproject.mid.ListObserver;
import org.servalproject.mid.Messaging;
import org.servalproject.mid.Observer;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.mid.networking.AbstractListObserver;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servaldna.meshms.MeshMSConversation;

/**
 * Created by jeremy on 18/07/16.
 */
public class Notifications {
	private final Context context;
	private final Identity id;
	private int hashCode = 0;
	private final int notificationId;
	private static final String TAG = "Notifications";
	private static final String NotificationTag = "PrivateMessaging";

	static void init(final Serval serval, final Context context) {
		serval.identities.listObservers.addBackground(new AbstractListObserver<Identity>() {
			@Override
			public void added(Identity obj) {
				new Notifications(context, obj);
			}
		});

		// track which interfaces are running, start a foreground service whenever an interface is up
		serval.knownPeers.interfaceObservers.addBackground(new AbstractListObserver<Interface>() {
			private boolean serviceStarted = false;

			@Override
			public void added(Interface obj) {
				updateNotification();
			}

			@Override
			public void removed(Interface obj) {
				updateNotification();
			}

			private void updateNotification() {
				boolean shouldRun = serval.knownPeers.getInterfaceCount() > 0;
				if (shouldRun == serviceStarted)
					return;
				Intent i = new Intent(context, ForegroundService.class);
				i.putExtra("foreground", shouldRun);
				context.startService(i);
				serviceStarted = shouldRun;
			}
		});
	}

	private static int nextId = 0;

	private Notifications(Context context, Identity id) {
		this.context = context;
		this.id = id;
		notificationId = ++nextId;
		id.messaging.observers.addBackground(new Observer<Messaging>() {
			@Override
			public void updated(Messaging obj) {
				updateNotification();
			}
		});
	}

	private void updateNotification() {
		int newHashCode = id.messaging.getHashCode();
		if (hashCode == newHashCode)
			return;
		hashCode = newHashCode;

		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		MeshMSConversation unread = null;
		int unreadCount = 0;
		for (MeshMSConversation conv : id.messaging.conversations) {
			if (!conv.isRead) {
				unread = conv;
				unreadCount++;
			}
		}

		if (unreadCount == 0) {
			nm.cancel(NotificationTag, notificationId);
		} else {

			Navigation key = Navigation.Inbox;
			Peer peer = null;
			if (unreadCount == 1) {
				key = Navigation.PrivateMessages;
				peer = Serval.getInstance().knownPeers.getPeer(unread.them);
			}

			Intent intent = MainActivity.getIntentFor(context, key, id, peer, null);
			PendingIntent pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

			NotificationCompat.Builder builder =
					new NotificationCompat.Builder(context)
							.setAutoCancel(true)
							.setSmallIcon(R.mipmap.serval_head)
							.setDefaults(Notification.DEFAULT_ALL)
							.setContentTitle(context.getString(R.string.private_messaging_title))
							.setContentText(context.getResources().getQuantityString(R.plurals.private_messages, unreadCount, id.getName(), unreadCount))
							.setContentIntent(pending);

			nm.notify(NotificationTag, this.notificationId, builder.build());
		}
	}

}
