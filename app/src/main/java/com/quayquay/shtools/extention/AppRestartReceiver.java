package com.quayquay.shtools.extention;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.quayquay.shtools.MainActivity;

public class AppRestartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent restartIntent = new Intent(context, MainActivity.class); // Thay MainActivity bằng Activity chính của bạn
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(restartIntent);
    }
}
