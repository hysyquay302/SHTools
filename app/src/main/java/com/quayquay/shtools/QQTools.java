package com.quayquay.shtools;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

import com.quayquay.hsq.tools.HSQConfig;

import java.util.Map;

public class QQTools {
    public static void startAuto(Class<?> service, Map<String, String> data){
        Intent intent = new Intent(HSQConfig.getContext(), service);
        if (data != null){
            for (String key : data.keySet()){
                intent.putExtra(key, data.get(key));
            }
        }
        HSQConfig.getContext().startForegroundService(intent);
    }
    public static String getCopy() {
        String clb = "";
        try {
            ClipboardManager clipboardManager = (ClipboardManager) HSQConfig.getContext().getSystemService(Context.CLIPBOARD_SERVICE);

            if (clipboardManager.hasPrimaryClip()) {
                try {

                    if (clipboardManager.getPrimaryClip().getItemCount() > 0) {
                        clb = clipboardManager.getPrimaryClip().getItemAt(0).getText().toString();
                        return  clb;
                    }
                } catch (Exception e) {}
            }
        }
        catch (Exception e) {   }
        return clb;
    }

    public static void clearClipboard() {
        ClipboardManager clipboardManager = (ClipboardManager) HSQConfig.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("", ""); // Tạo ClipData trống
        clipboardManager.setPrimaryClip(clip); // Đặt ClipData trống vào clipboard chính
    }

}
