package com.quayquay.shtools.extention;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import java.io.File;

public class AppInstaller {
    public static void installApk(Context context, String apkFilePath) {
        File apkFile = new File(apkFilePath);
        if (apkFile.exists()) {
            try {
                Uri apkUri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".FileProvider", // Phải trùng khớp với AndroidManifest.xml
                        apkFile
                );

                // 1. Sửa ACTION_INSTALL_PACKAGE thành ACTION_VIEW
                Intent intent = new Intent(Intent.ACTION_VIEW);

                // 2. Ép kiểu dữ liệu (MIME Type) CỰC KỲ QUAN TRỌNG để máy nó tự hiểu là file APK
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");

                // 3. Cấp quyền cho trình cài đặt đọc file qua Uri
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);
            } catch (Exception e) {
                new AlertDialog.Builder(context)
                        .setTitle("Lỗi")
                        .setMessage("Không cài được: " + e.getMessage())
                        .setPositiveButton("OK", null)
                        .show();
            }
        }
    }
}