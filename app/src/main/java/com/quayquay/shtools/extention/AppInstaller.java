package com.quayquay.shtools.extention;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import java.io.File;

public class AppInstaller {
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void installApk(Context context, String apkFilePath) {
        File apkFile = new File(apkFilePath);
        if (apkFile.exists()) {
            try {
                Uri apkUri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".FileProvider", // Thay thế bằng authority của bạn
                        apkFile
                );
                Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                intent.setData(apkUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                new AlertDialog.Builder(context)
                        .setTitle("Lỗi")
                        .setMessage("không cài được")
                        .setPositiveButton("OK", null)
                        .show();
            }
        }
    }
}
