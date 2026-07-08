package com.quayquay.shtools.extention;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

public class AppInstaller {
    public static void installApk(Context context, String apkFilePath) {
        File apkFile = new File(apkFilePath);
        if (!apkFile.exists()) return;

        try {
            Uri apkUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".FileProvider",
                    apkFile
            );

            try {
                context.startActivity(buildInstallIntent(Intent.ACTION_INSTALL_PACKAGE, apkUri));
            } catch (Exception firstError) {
                Log.w("AppInstaller", "ACTION_INSTALL_PACKAGE failed, fallback ACTION_VIEW", firstError);
                context.startActivity(buildInstallIntent(Intent.ACTION_VIEW, apkUri));
            }
        } catch (Exception e) {
            Log.e("AppInstaller", "Cannot install apk", e);
            Toast.makeText(context, "Không cài được APK: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static Intent buildInstallIntent(String action, Uri apkUri) {
        Intent intent = new Intent(action);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, false);
        return intent;
    }
}
