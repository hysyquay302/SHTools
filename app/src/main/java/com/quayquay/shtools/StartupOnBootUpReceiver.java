package com.quayquay.shtools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartupOnBootUpReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(Intent.ACTION_BOOT_COMPLETED.equals(action) || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setClass(context, MainActivity.class);
            context.startActivity(i);
            return;
        }
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            startMainActivity(context);
            return;
        }
        if (Intent.ACTION_PACKAGE_REPLACED.equals(action) && intent.getData() != null) {
            String packageName = intent.getData().getSchemeSpecificPart();
            if (packageName.equals(context.getPackageName())) {
                // Ứng dụng của bạn đã được cập nhật, khởi động lại Activity chính
                startMainActivity(context);
            }
        }
    }

    private void startMainActivity(Context context) {
        Intent restartIntent = new Intent(context, MainActivity.class); // Thay MainActivity bằng Activity chính của bạn
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(restartIntent);
    }
}
