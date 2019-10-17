package org.servalproject.mid.networking;

import android.content.Context;
import android.content.Intent;

public interface GlobalReceiverCallBack {
    void onCallBackReceived(Context context, Intent intent);
}
