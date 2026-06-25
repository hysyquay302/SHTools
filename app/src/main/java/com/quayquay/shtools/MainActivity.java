package com.quayquay.shtools;

import static com.quayquay.shtools.extention.ASUtils.delay;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.quayquay.shtools.extention.ActivityVisibilityObserver;
import com.quayquay.shtools.services.ApiAccessibilityService;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private AutoCompleteTextView apiKeyAutoComplete;
    private java.util.List<ApiKeyItem> apiKeyList;
    private String selectedApiKey = "";
    public static class ApiKeyItem {
        private String alias;
        private String apiKey;
        public ApiKeyItem(String alias, String apiKey) {
            this.alias = alias;
            this.apiKey = apiKey;
        }
        public String getAlias() { return alias; }
        public String getApiKey() { return apiKey; }
        @Override
        public String toString() { return alias; }
    }
    SharedPreferences sharedPreferences;
    Button btnSave;
    Button btnClear;
    Button btnResetPromt;
    SharedPreferences.Editor editor;

    // --- Các biến cờ trạng thái quyền ---
    private boolean isDeviceOwnerGranted = false;
    private boolean isAccessibilityEnabled = false;
    private boolean isMediaProjectionGranted = false;
    private boolean isNotificationPermissionGranted = false;
    private boolean isReadPhoneStatePermissionGranted = false;
    private boolean areStoragePermissionsGranted = false;
    private boolean isOverlayPermissionGranted = false;
    private boolean isInstallPermissionGranted = false;
    private boolean isWriteSettingsPermissionGranted = false;
    private boolean hasShownPermissionDialog = false;

    // --- Các biến Launcher ---
    private ActivityResultLauncher<Intent> mediaProjectionLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private ActivityResultLauncher<Intent> accessibilityLauncher;
    private ActivityResultLauncher<Intent> overlayLauncher;
    private ActivityResultLauncher<Intent> installLauncher;
    private ActivityResultLauncher<Intent> writeSettingsLauncher;
    private ActivityResultLauncher<String> readPhoneStateLauncher;
    private ActivityResultLauncher<String[]> storageLauncher;

    public static String apiKeyServer;

    // Biến static để StartAuto hoặc HSQLibrary dễ dàng gọi và sử dụng
    public static Intent sMediaProjectionData;
    public static int sMediaProjectionResultCode;

    private ActivityVisibilityObserver observer;
    private boolean isOnClickRegisterCalled = false;
    private final Handler handler = new Handler(Looper.getMainLooper()); // Fix: may be 'final'

    private void initPreferences() {
        // Fix: 'getDefaultSharedPreferences' is deprecated
        sharedPreferences = getSharedPreferences("QQ_PREFS_DATA", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSharedPreferences("QQ_PREFS", Context.MODE_PRIVATE).edit().putBoolean("isForceStopped", false).apply();
        initPreferences();

        Button btnRemoveAdmin = findViewById(R.id.btn_remove_admin);
        btnRemoveAdmin.setOnClickListener(v -> showConfirmationDialog());

        Button btnDeleteChat = findViewById(R.id.btn_delete_chat);
        btnDeleteChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 🌟 SỬA LỖI ĐỎ: Gọi đích danh MainActivity.this
                File historyFolder = new File(MainActivity.this.getFilesDir(), "Survey");
                File historyFile = new File(historyFolder, "chat_history_zone.json");

                if (historyFile.exists()) {
                    boolean isDeleted = historyFile.delete();
                    if (isDeleted) {
                        // Xóa thành công thì báo thành công
                        Toast.makeText(MainActivity.this, "Đã xóa sạch trí nhớ của AI!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Xóa xịt thì báo lỗi
                        Toast.makeText(MainActivity.this, "Lỗi: Đéo thể xóa được file lịch sử!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // File không tồn tại tức là chat đang trống
                    Toast.makeText(MainActivity.this, "Lịch sử chat đang trống sẵn rồi sếp!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnSave = findViewById(R.id.btn_save);
        btnClear = findViewById(R.id.btn_clear);
        btnResetPromt = findViewById(R.id.btn_reset_promt);

        // --- Bắt đầu Setup Dropdown ---
        apiKeyAutoComplete = findViewById(R.id.apiKeyAutoComplete);
        apiKeyList = loadApiKeysFromFile();
        migrateOldApiKey(); // Chuyển DATA cũ sang SAVED_API_KEY

        android.widget.ArrayAdapter<ApiKeyItem> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, apiKeyList);
        apiKeyAutoComplete.setAdapter(adapter);

        apiKeyAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            ApiKeyItem selectedItem = (ApiKeyItem) parent.getItemAtPosition(position);
            selectedApiKey = selectedItem.getApiKey();
        });

        loadSavedSelection();

        btnSave.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnResetPromt.setOnClickListener(v -> showResetPromtDialog());

        hasShownPermissionDialog = false;

        // 1. Accessibility Launcher
        accessibilityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> checkAccessibility()
        );

        // 2. Media Projection Launcher
        mediaProjectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        sMediaProjectionResultCode = result.getResultCode();
                        sMediaProjectionData = result.getData();

                        isMediaProjectionGranted = true;
                        hasShownPermissionDialog = false;

                        checkNextPermission();
                    } else if (!hasShownPermissionDialog) {
                        showMediaProjectionDialog();
                    }
                });

        // 3. Notification Launcher
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        isNotificationPermissionGranted = true;
                        hasShownPermissionDialog = false;
                        checkNextPermission();
                    } else if (!hasShownPermissionDialog) {
                        showNotificationPermissionDialog();
                    }
                });

        // Đăng ký các quyền khác (Overlay, Install, Write Settings, Phone State, Storage)
        try {
            overlayLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (Settings.canDrawOverlays(this)) {
                            isOverlayPermissionGranted = true;
                            checkNextPermission();
                        } else if (!hasShownPermissionDialog) {
                            showOverlayPermissionDialog();
                        }
                    }
            );
        } catch (Exception e) { delay(1000); }

        try {
            installLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (getPackageManager().canRequestPackageInstalls()) {
                            isInstallPermissionGranted = true;
                            checkNextPermission();
                        } else if (!hasShownPermissionDialog) {
                            showInstallPermissionDialog();
                        }
                    }
            );
        } catch (Exception e) { delay(1000); }

        try {
            writeSettingsLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (Settings.System.canWrite(this)) {
                            isWriteSettingsPermissionGranted = true;
                            checkNextPermission();
                        } else if (!hasShownPermissionDialog) {
                            showWriteSettingsPermissionDialog();
                        }
                    }
            );
        } catch (Exception e) { delay(1000); }

        try {
            readPhoneStateLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            isReadPhoneStatePermissionGranted = true;
                            checkNextPermission();
                        } else if (!hasShownPermissionDialog) {
                            showReadPhoneStatePermissionDialog();
                        }
                    }
            );
        } catch (Exception e) { delay(1000); }

        try{
            storageLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        boolean allGranted = true;
                        for (boolean isGranted : result.values()) {
                            if (!isGranted) {
                                allGranted = false;
                                break;
                            }
                        }
                        if (allGranted) {
                            areStoragePermissionsGranted = true;
                            checkNextPermission();
                        } else if (!hasShownPermissionDialog) {
                            showStoragePermissionDialog();
                        }
                    }
            );
        } catch (Exception e) { delay(1000); }

        observer = new ActivityVisibilityObserver();
        getLifecycle().addObserver(observer);
        // --- THÊM ĐOẠN NÀY ĐỂ MỞ SOCKET TỰ ĐỘNG ---
        android.content.SharedPreferences prefs = getSharedPreferences("QQ_PREFS", android.content.Context.MODE_PRIVATE);
        String savedDeviceId = prefs.getString("saved_device_id", null);
        if (savedDeviceId != null) {
            RemoteStreamManager.getInstance(this, savedDeviceId);
        }

        // Bắt đầu chuỗi kiểm tra quyền
        checkDeviceOwner();
    }

    // ==========================================
    // CÁC HÀM KIỂM TRA QUYỀN (CHECKERS)
    // ==========================================

    private void checkAccessibility() {
        if (isAccessibilityServiceEnabled()) {
            isAccessibilityEnabled = true;
            checkNextPermission();
        } else if (!hasShownPermissionDialog) {
            showAccessibilityRequestDialog();
        }
    }

    private void checkMediaProjection() {
        if (!isMediaProjectionGranted) {
            MediaProjectionManager mm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (mm != null) {
                mediaProjectionLauncher.launch(mm.createScreenCaptureIntent());
            }
        } else {
            checkNextPermission();
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                if (!hasShownPermissionDialog) {
                    showNotificationPermissionDialog();
                }
            } else {
                isNotificationPermissionGranted = true;
                checkNextPermission();
            }
        } else {
            isNotificationPermissionGranted = true;
            checkNextPermission();
        }
    }

    private void checkReadPhoneStatePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (!hasShownPermissionDialog) {
                showReadPhoneStatePermissionDialog();
            }
        } else {
            isReadPhoneStatePermissionGranted = true;
            checkNextPermission();
        }
    }

    private void showConfirmationDialog() {
        // Fix: Replace with lambda
        new AlertDialog.Builder(this)
                .setTitle("CẢNH BÁO")
                .setMessage("Anh có chắc chắn muốn HỦY quyền Device Owner không?\n\nNếu hủy, Tool sẽ không thể tự động xử lý các tác vụ quản trị nữa!")
                .setPositiveButton("Hủy Quyền", (dialog, which) -> removeDeviceOwner())
                .setNegativeButton("Quay lại", null)
                .show();
    }

    @SuppressWarnings("deprecation") // Ẩn cảnh báo gỡ quyền Owner (do Google đã gạch ngang từ API 26)
    private void removeDeviceOwner() {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm != null && dpm.isDeviceOwnerApp(getPackageName())) {
                dpm.clearDeviceOwnerApp(getPackageName());
                Toast.makeText(this, "Đã Hủy quyền Admin thành công!", Toast.LENGTH_LONG).show();
                Log.d("Admin", "Đã hủy quyền thành công!");
            } else {
                Toast.makeText(this, "Tool hiện chưa được cấp quyền Admin!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("Admin", "Lỗi khi hủy quyền: " + e.getMessage());
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkFileAccess() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (!hasShownPermissionDialog) {
                showStoragePermissionDialog();
            }
        } else {
            areStoragePermissionsGranted = true;
            checkNextPermission();
        }
    }

    private void checkDisplayOverOtherApps() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            overlayLauncher.launch(intent);
        } else {
            isOverlayPermissionGranted = true;
            checkNextPermission();
        }
    }

    private void checkInstallUnknownSources() {
        if (!getPackageManager().canRequestPackageInstalls()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName()));
            installLauncher.launch(intent);
        } else {
            isInstallPermissionGranted = true;
            checkNextPermission();
        }
    }

    private void checkWriteSettingsPermission() {
        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            writeSettingsLauncher.launch(intent);
        } else {
            isWriteSettingsPermissionGranted = true;
            checkNextPermission();
        }
    }

    // ==========================================
    // CHUỖI ĐIỀU HƯỚNG QUYỀN (ROUTER)
    // ==========================================

    public void checkNextPermission() {
        hasShownPermissionDialog = false;
        if (!isDeviceOwnerGranted) {
            checkDeviceOwner();
        }
        else if (!isAccessibilityEnabled) {
            checkAccessibility();
        } else if (!isMediaProjectionGranted) {
            checkMediaProjection();
        } else if (!isNotificationPermissionGranted) {
            checkNotificationPermission();
        } else if (!isReadPhoneStatePermissionGranted) {
            checkReadPhoneStatePermission();
        } else if (!areStoragePermissionsGranted) {
            checkFileAccess();
        } else if (!isOverlayPermissionGranted) {
            checkDisplayOverOtherApps();
        } else if (!isInstallPermissionGranted) {
            checkInstallUnknownSources();
        } else if (!isWriteSettingsPermissionGranted) {
            checkWriteSettingsPermission();
        } else {
            // Tất cả quyền đã OK!
            startMainApplicationLogic();
        }
    }

    // ==========================================
    // CÁC HÀM HIỂN THỊ HỘP THOẠI (DIALOGS)
    // ==========================================

    private void showAccessibilityRequestDialog() {
        hasShownPermissionDialog = true;
        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu Accessibility")
                .setMessage("Vui lòng bật Accessibility cho ứng dụng để sử dụng đầy đủ tính năng.")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    accessibilityLauncher.launch(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMediaProjectionDialog() {
        hasShownPermissionDialog = true;
        // Fix: Expression lambda
        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu quyền")
                .setMessage("Vui lòng cấp quyền Quay/Chụp màn hình. Thư viện HSQLibrary cần quyền này để thu thập dữ liệu màn hình.")
                .setPositiveButton("OK", (dialog, which) -> checkMediaProjection())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showNotificationPermissionDialog() {
        hasShownPermissionDialog = true;
        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu quyền")
                .setMessage("Vui lòng cấp quyền Thông báo để ứng dụng có thể hiển thị trạng thái hoạt động chạy ngầm (Foreground Service).")
                .setPositiveButton("OK", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showReadPhoneStatePermissionDialog() {
        hasShownPermissionDialog = true;
        // Fix: Expression lambda
        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu quyền")
                .setMessage("Vui lòng cấp quyền đọc trạng thái điện thoại để sử dụng đầy đủ tính năng.")
                .setPositiveButton("OK", (dialog, which) -> readPhoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStoragePermissionDialog() {
        hasShownPermissionDialog = true;
        // Fix: Expression lambda
        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu quyền")
                .setMessage("Vui lòng cấp quyền truy cập tệp và bộ nhớ để sử dụng đầy đủ tính năng.")
                .setPositiveButton("OK", (dialog, which) -> storageLauncher.launch(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showOverlayPermissionDialog() {
        hasShownPermissionDialog = true;
        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu quyền")
                .setMessage("Vui lòng cấp quyền hiển thị trên các ứng dụng khác để sử dụng đầy đủ tính năng.")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    overlayLauncher.launch(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showInstallPermissionDialog() {
        hasShownPermissionDialog = true;
        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu quyền")
                .setMessage("Vui lòng cấp quyền cài đặt ứng dụng từ nguồn không xác định để sử dụng đầy đủ tính năng.")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + getPackageName()));
                    installLauncher.launch(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showWriteSettingsPermissionDialog() {
        hasShownPermissionDialog = true;
        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu quyền")
                .setMessage("Ứng dụng cần quyền WRITE_SETTINGS để thay đổi cài đặt hệ thống.")
                .setPositiveButton("Cấp quyền", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    writeSettingsLauncher.launch(intent);
                })
                .setNegativeButton("Từ chối", null)
                .show();
    }
    private void startMainApplicationLogic() {
        // Xóa sạch các bộ đếm ngược cũ (nếu có) để tránh lỗi đếm lùi nhiều lần
        handler.removeCallbacks(onClickRegisterRunnable);

        // Bắt đầu đếm đúng 60s (60000ms)
        handler.postDelayed(onClickRegisterRunnable, 60000);

        // Báo cho sếp biết là đã cấp full quyền và đang đếm lùi
        Toast.makeText(this, "Đã đủ quyền! Tool sẽ tự động chạy sau 60s...", Toast.LENGTH_LONG).show();
    }

    private boolean isAccessibilityServiceEnabled() {
        String prefString = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (prefString != null) {
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
            splitter.setString(prefString);
            while (splitter.hasNext()) {
                String componentName = splitter.next();
                if (componentName.equals(getPackageName() + "/" + ApiAccessibilityService.class.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private final Runnable onClickRegisterRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                // 🛡️ LỚP BỌC THÉP SỐ 2: TẮT BOM NẾU TOOL ĐANG CHẠY HOẶC ĐANG UPDATE
                // Đảm bảo sếp đã khai báo 2 biến static này bên StartAuto.java nhé!
                if (StartAuto.isToolRunning || StartAuto.isUpdating) {
                    Log.d("MainActivity", "Hủy nổ bom 60s vì Tool đang chạy hoặc Update!");
                    finishAffinity(); // Giết sạch sành sanh Activity này đi
                    return;
                }

                if (!isOnClickRegisterCalled && observer.isActivityStarted()) {
                    startAutoServiceWithMediaProjection();
                } else {
                    handler.postDelayed(this, 2000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public void onClickRegister(View view) {
        // HỦY DIỆT CÁI ĐỒNG HỒ ĐẾM NGƯỢC 60S NGAY LẬP TỨC
        handler.removeCallbacks(onClickRegisterRunnable);

        isOnClickRegisterCalled = true;
        if (isAccessibilityEnabled) {
            startAutoServiceWithMediaProjection();
        } else {
            hasShownPermissionDialog = false;
            checkNextPermission();
        }
    }

    // =========================================================
    // HÀM MỚI: START SERVICE KÈM THEO CHÌA KHÓA CHỤP MÀN HÌNH
    // =========================================================
    private void startAutoServiceWithMediaProjection() {
        // Fix: Method invocation 'toString' may produce 'NullPointerException'
        apiKeyServer = selectedApiKey;
        if (!TextUtils.isEmpty(apiKeyServer)) {
            // 1. Cài đặt thông số giao diện cho Notification của HSQService
            com.quayquay.hsq.config.HSQUiParams uiParams = new com.quayquay.hsq.config.HSQUiParams(
                    "SHTools",
                    BuildConfig.VERSION_NAME,
                    android.R.drawable.ic_dialog_info
            );
            com.quayquay.hsq.tools.HSQServiceManager.setUiParams(uiParams);

            // 2. Tạo xe rùa (Intent) để chở dữ liệu sang StartAuto
            Intent serviceIntent = new Intent(MainActivity.this, StartAuto.class);
            serviceIntent.putExtra("api_key", apiKeyServer);

            // 3. Ném chìa khóa chụp màn hình lên xe rùa!
            if (sMediaProjectionData != null) {
                serviceIntent.putExtra("resultCode", sMediaProjectionResultCode);
                serviceIntent.putExtra("data", sMediaProjectionData);
            }

            // 4. Kích hoạt Service (Không cần check version O vì minSdk là 28)
            startForegroundService(serviceIntent);

            finishAffinity();
        } else {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Lỗi")
                    .setMessage("Chưa có API Key")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void checkDeviceOwner() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm != null && dpm.isDeviceOwnerApp(getPackageName())) {
            isDeviceOwnerGranted = true;
            checkNextPermission();
        } else if (!hasShownPermissionDialog) {
            showDeviceOwnerRequestDialog();
        }
    }

    private void showDeviceOwnerRequestDialog() {
        hasShownPermissionDialog = true;

        // Lấy deviceID thật, nếu có thì thay vào lệnh, không thì giữ placeholder
        String realDeviceId = null;
        try {
            realDeviceId = StartAuto.deviceID;
        } catch (Exception ignored) { }

        String devicePlaceholder = (realDeviceId != null && !realDeviceId.isEmpty() && !realDeviceId.equals("UNKNOWN"))
                ? realDeviceId
                : "{deviceID}";

        String adbCommand = "adb -s " + devicePlaceholder + " shell dpm set-device-owner " + getPackageName() + "/.MyDeviceAdminReceiver" +
                " && adb -s " + devicePlaceholder + " shell pm grant " + getPackageName() + " android.permission.WRITE_SECURE_SETTINGS" +
                " && adb -s " + devicePlaceholder + " shell appops set " + getPackageName() + " SYSTEM_ALERT_WINDOW allow";

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Yêu cầu Quyền Quản Trị Tối Cao (Device Owner)")
                .setMessage("Vui lòng cắm cáp vào máy tính và chạy lệnh ADB sau:\n\n" + adbCommand)
                .setPositiveButton("Tôi đã chạy lệnh", (d, which) -> {
                    hasShownPermissionDialog = false;
                    checkDeviceOwner();
                })
                .setNegativeButton("Copy Lệnh", null)
                .setCancelable(false)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnCopy = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            btnCopy.setOnClickListener(v -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("ADB Command", adbCommand);
                if (clipboard != null) clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "Đã copy lệnh vào bộ nhớ tạm", Toast.LENGTH_SHORT).show();
            });
        });

        dialog.show();
    }

    @Override
    public void onClick(View view) {
        if (view == btnSave) {
            if (!selectedApiKey.isEmpty()) {
                editor.putString("SAVED_API_KEY", selectedApiKey);
                editor.commit(); // Dùng commit() theo code cũ của sếp
                Toast.makeText(this, "Đã lưu API Key thành công!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Vui lòng chọn một tài khoản từ danh sách", Toast.LENGTH_SHORT).show();
            }
        } else if (view == btnClear) {
            apiKeyAutoComplete.setText("", false);
            selectedApiKey = "";
            editor.remove("SAVED_API_KEY");
            editor.commit();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        // Ngắt đếm ngược nếu người dùng ẩn app (bấm phím Home)
        handler.removeCallbacks(onClickRegisterRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Thoát app là hủy luôn bộ đếm
        handler.removeCallbacks(onClickRegisterRunnable);
    }
    // ==========================================
    // CÁC HÀM XỬ LÝ DROPDOWN VÀ JSON
    // ==========================================
    private java.util.List<ApiKeyItem> loadApiKeysFromFile() {
        java.util.List<ApiKeyItem> items = new java.util.ArrayList<>();
        try {
            java.io.InputStream inputStream = getResources().openRawResource(R.raw.api_keys);
            String jsonString = new java.util.Scanner(inputStream).useDelimiter("\\A").next();
            org.json.JSONArray jsonArray = new org.json.JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                org.json.JSONObject jsonObject = jsonArray.getJSONObject(i);
                String alias = jsonObject.getString("alias");
                String apiKey = jsonObject.getString("apiKey");

                if (apiKey.equals("KEY_SERVEY")) {
                    apiKey = BuildConfig.API_SERVEY;
                }
                else if (apiKey.equals("KEY_SERVEY_2")) {
                    apiKey = BuildConfig.API_SERVEY_2;
                }
                else if (apiKey.equals("KEY_SERVEY_TEST")) {
                    apiKey = BuildConfig.API_SERVEY_TEST;
                }

                items.add(new ApiKeyItem(alias, apiKey));
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Lỗi đọc file api_keys.json", e);
        }
        return items;
    }

    private void migrateOldApiKey() {
        String oldApiKey = sharedPreferences.getString("DATA", null);
        if (oldApiKey != null && !sharedPreferences.contains("SAVED_API_KEY")) {
            for (ApiKeyItem item : apiKeyList) {
                if (item.getApiKey().equals(oldApiKey)) {
                    editor.putString("SAVED_API_KEY", item.getApiKey());
                    editor.remove("DATA");
                    editor.apply();
                    return;
                }
            }
        }
    }

    private void loadSavedSelection() {
        String savedKey = sharedPreferences.getString("SAVED_API_KEY", null);
        if (savedKey != null) {
            for (ApiKeyItem item : apiKeyList) {
                if (item.getApiKey().equals(savedKey)) {
                    selectedApiKey = item.getApiKey();
                    apiKeyAutoComplete.setText(item.getAlias(), false);
                    break;
                }
            }
        }
    }
    private void showResetPromtDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Promt?")
                .setMessage("Tool sẽ xóa version prompt đang lưu và thử xóa file prompt cũ.\nLần updatePromt tiếp theo sẽ tải lại prompt mới. Tiếp tục không?")
                .setPositiveButton("RESET", (dialog, which) -> resetPromtState())
                .setNegativeButton("HỦY", null)
                .show();
    }

    private void resetPromtState() {
        boolean saved = sharedPreferences.edit().putInt("PROMT_VERSION", 0).commit();

        File promtFile = new File("/sdcard/Servey/PromtGem.txt");
        boolean deleted = !promtFile.exists() || promtFile.delete();

        if (saved && deleted) {
            Toast.makeText(this, "Đã reset Promt. Lần updatePromt tới sẽ tải lại file mới.", Toast.LENGTH_LONG).show();
        } else if (saved) {
            Toast.makeText(this, "Đã reset version Promt nhưng chưa xóa được file cũ.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Lỗi: Không reset được version Promt.", Toast.LENGTH_LONG).show();
        }
    }
}