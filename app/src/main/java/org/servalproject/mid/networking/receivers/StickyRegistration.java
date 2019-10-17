package org.servalproject.mid.networking.receivers;

import android.content.BroadcastReceiver;
import android.content.Intent;

public class StickyRegistration {
    /**
     * the {@link BroadcastReceiver} to unregister later
     */
    public final BroadcastReceiver receiver;

    /**
     * the {@link Intent}
     */
    public final Intent intent;

    StickyRegistration(BroadcastReceiver receiver, Intent intent) {
        this.receiver = receiver;
        this.intent = intent;
    }
}
