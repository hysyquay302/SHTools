package com.quayquay.shtools; // Đổi theo package của anh

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.content.Intent;
import android.os.Handler;
import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;

import io.socket.client.Socket;

public class RemoteStreamManager {
    private static final String TAG = "RemoteStreamManager";
    @android.annotation.SuppressLint("StaticFieldLeak")
    private static RemoteStreamManager instance;
    private final Context context;
    private Socket mSocket;
    private final String deviceId;
    private android.os.HandlerThread streamThread;
    private Handler streamHandler;
    private final ByteArrayOutputStream reusableOutputStream = new ByteArrayOutputStream();
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private boolean isStreaming = false;
    private long lastFrameTime = 0;
    private boolean streamRequested = false;

    // URL Cloudflare của anh
    private static final String SERVER_URL = "https://quaykute.id.vn";

    public static RemoteStreamManager getInstance(Context context, String deviceId) {
        if (instance == null) {
            instance = new RemoteStreamManager(context.getApplicationContext(), deviceId);
        }
        return instance;
    }

    private RemoteStreamManager(Context context, String deviceId) {
        this.context = context;
        this.deviceId = deviceId;
        initSocket();
    }

    private void initSocket() {
        try {
            okhttp3.OkHttpClient okHttpClient = new okhttp3.OkHttpClient.Builder()
                    .proxy(java.net.Proxy.NO_PROXY)
                    .build();
            io.socket.client.IO.Options options = new io.socket.client.IO.Options();
            options.callFactory = okHttpClient;
            options.webSocketFactory = okHttpClient;
            options.transports = new String[]{"websocket"};
            // Truyền options vào hàm khởi tạo socket
            mSocket = io.socket.client.IO.socket(SERVER_URL, options);
            // ------------------------------------------------------------------------
            mSocket.on(io.socket.client.Socket.EVENT_CONNECT, args -> android.util.Log.d(TAG, "Socket Connected!"))
                    .on("start_stream", args -> {
                        try {
                            JSONObject data = (JSONObject) args[0];
                            if (data.getString("device_id").equals(deviceId)) {
                                streamRequested = true; // Lưu lại yêu cầu
                                startStream();
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    })
                    .on("apk_stop_stream", args -> {
                        try {
                            JSONObject data = (JSONObject) args[0];
                            if (data.getString("device_id").equals(deviceId)) {
                                streamRequested = false; // Xóa yêu cầu
                                stopStream();
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    })
                    .on("apk_receive_volume", args -> {
                        try {
                            JSONObject data = (JSONObject) args[0];
                            if (data.getString("device_id").equals(deviceId)) {
                                handleVolumeCommand(data.getString("type"));
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    })
                    .on("apk_receive_action", args -> {
                        try {
                            JSONObject data = (JSONObject) args[0];
                            if (data.getString("device_id").equals(deviceId)) {
                                handleControlAction(data.getString("action"));
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    })
                    .on("apk_receive_tap", args -> {
                        try {
                            JSONObject data = (JSONObject) args[0];
                            if (data.getString("device_id").equals(deviceId)) {
                                dispatchTap((float) data.getDouble("x"), (float) data.getDouble("y"));
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    })
                    .on("apk_receive_swipe", args -> {
                        try {
                            JSONObject data = (JSONObject) args[0];
                            if (data.getString("device_id").equals(deviceId)) {
                                dispatchSwipe((float) data.getDouble("x1"), (float) data.getDouble("y1"),
                                        (float) data.getDouble("x2"), (float) data.getDouble("y2"));
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    })
                    .on("apk_receive_text", args -> {
                        try {
                            JSONObject data = (JSONObject) args[0];
                            if (data.getString("device_id").equals(deviceId)) {
                                handleTextInput(data.optString("text", ""), data.optBoolean("clear", false));
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    });
            mSocket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void startStream() {
        if (isStreaming || MainActivity.sMediaProjectionData == null) return;
        try {
            // KHỞI ĐỘNG DỊCH VỤ NGẦM TRƯỚC
            android.content.Intent serviceIntent = new android.content.Intent(context, com.quayquay.shtools.services.StreamService.class);
            context.startForegroundService(serviceIntent);
            try { Thread.sleep(100); } catch (Exception ignored) {}

            MediaProjectionManager mpm = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mediaProjection = mpm.getMediaProjection(MainActivity.sMediaProjectionResultCode, MainActivity.sMediaProjectionData);
            if (mediaProjection == null) return;
            isStreaming = true;

            // TẠO LUỒNG RIÊNG CHO VIỆC XỬ LÝ HÌNH ẢNH - KHÔNG CHẶN MAIN THREAD
            streamThread = new android.os.HandlerThread("ScreenStreamThread");
            streamThread.start();
            streamHandler = new Handler(streamThread.getLooper());

            int realWidth = android.content.res.Resources.getSystem().getDisplayMetrics().widthPixels;
            int realHeight = android.content.res.Resources.getSystem().getDisplayMetrics().heightPixels;

            int width = 360;
            int height = (int) (360.0f * realHeight / realWidth);
            int density = context.getResources().getDisplayMetrics().densityDpi;

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "ScreenStream",
                    width, height, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(), null, null
            );

            // CHẠY LISTENER TRÊN LUỒNG RIÊNG thay vì MainLooper
            imageReader.setOnImageAvailableListener(reader -> {
                if (!isStreaming) return;
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastFrameTime < 80) return; // 80ms = ~12 FPS
                        lastFrameTime = currentTime;

                        Image.Plane[] planes = image.getPlanes();
                        int rowStride = planes[0].getRowStride();
                        int pixelStride = planes[0].getPixelStride();
                        int rowPadding = rowStride - pixelStride * width;

                        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(planes[0].getBuffer());
                        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);

                        // TÁI SỬ DỤNG BUFFER THAY VÌ TẠO MỚI MỖI FRAME
                        reusableOutputStream.reset();
                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 20, reusableOutputStream);
                        String base64Frame = Base64.encodeToString(reusableOutputStream.toByteArray(), Base64.NO_WRAP);

                        JSONObject data = new JSONObject();
                        data.put("device_id", deviceId);
                        data.put("frame", base64Frame);
                        mSocket.emit("apk_frame", data);

                        croppedBitmap.recycle();
                        bitmap.recycle();
                    }
                } catch (Exception e) {
                    android.util.Log.w(TAG, "Lỗi xử lý frame: " + e.getMessage());
                } finally {
                    if (image != null) image.close();
                }
            }, streamHandler); // <-- streamHandler thay vì MainLooper
        } catch (Exception e) {
            isStreaming = false;
            android.util.Log.e(TAG, "LỖI KHỞI TẠO QUAY MÀN HÌNH: " + e.getMessage(), e);
            context.stopService(new android.content.Intent(context, com.quayquay.shtools.services.StreamService.class));
        }
    }

    private void stopStream() {
        isStreaming = false;
        if (virtualDisplay != null) virtualDisplay.release();
        if (imageReader != null) imageReader.close();
        if (mediaProjection != null) mediaProjection.stop();
        // DỌN DẸP LUỒNG RIÊNG
        if (streamThread != null) {
            streamThread.quitSafely();
            streamThread = null;
            streamHandler = null;
        }
        context.stopService(new android.content.Intent(context, com.quayquay.shtools.services.StreamService.class));
    }

    private void handleVolumeCommand(String type) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if ("UP".equals(type)) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
        } else if ("DOWN".equals(type)) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
        }
    }

    private void handleControlAction(String action) {
        android.accessibilityservice.AccessibilityService asbl = com.quayquay.shtools.services.ApiAccessibilityService.getInstance();
        if (asbl != null) {
            if ("HOME".equals(action)) asbl.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
            else if ("BACK".equals(action)) asbl.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
            else if ("RECENTS".equals(action)) asbl.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS);
        }
    }

    private void handleTextInput(String text, boolean clear) {
        try {
            if (clear) {
                Intent intent = new Intent("ADB_CLEAR_TEXT");
                intent.setPackage("com.android.adbkeyboard");
                context.sendBroadcast(intent);
                android.util.Log.d(TAG, "=> NHAN LENH XOA TEXT TU WEB");
                return;
            }

            if (text == null || text.isEmpty()) return;

            String safeText = text.length() > 4000 ? text.substring(0, 4000) : text;
            Intent intent = new Intent("ADB_INPUT_B64");
            intent.setPackage("com.android.adbkeyboard");
            String b64 = Base64.encodeToString(safeText.getBytes("UTF-8"), Base64.NO_WRAP);
            intent.putExtra("msg", b64);
            context.sendBroadcast(intent);
            android.util.Log.d(TAG, "=> NHAN LENH NHAP TEXT TU WEB: " + safeText.length() + " ky tu");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Loi nhap text qua ADB Keyboard: " + e.getMessage(), e);
        }
    }

    public void retryStream() {
        if (streamRequested) {
            startStream();
        }
    }
    private void dispatchTap(float percentX, float percentY) {
        android.accessibilityservice.AccessibilityService asbl = com.quayquay.shtools.services.ApiAccessibilityService.getInstance();
        if (asbl == null) return;

        // DÙNG REAL METRICS ĐỂ LẤY KÍCH THƯỚC GỐC BAO GỒM CẢ THANH ĐIỀU HƯỚNG
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        android.view.WindowManager wm = (android.view.WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            wm.getDefaultDisplay().getRealMetrics(metrics);
        } else {
            metrics = android.content.res.Resources.getSystem().getDisplayMetrics();
        }

        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int x = (int) (percentX * width);
        int y = (int) (percentY * height);

        android.util.Log.d(TAG, "=> NHẬN LỆNH CLICK TỪ WEB: Bản gốc (" + percentX + ", " + percentY + ") -> Chuyển thành thực tế (" + x + "px, " + y + "px)");

        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(x, y);
        android.accessibilityservice.GestureDescription.Builder builder = new android.accessibilityservice.GestureDescription.Builder();
        builder.addStroke(new android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 80)); // Cho chạm 80ms cho chắc nịch
        asbl.dispatchGesture(builder.build(), null, null);
    }

    private void dispatchSwipe(float pX1, float pY1, float pX2, float pY2) {
        android.accessibilityservice.AccessibilityService asbl = com.quayquay.shtools.services.ApiAccessibilityService.getInstance();
        if (asbl == null) return;

        // DÙNG REAL METRICS ĐỂ LẤY KÍCH THƯỚC GỐC BAO GỒM CẢ THANH ĐIỀU HƯỚNG
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        android.view.WindowManager wm = (android.view.WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            wm.getDefaultDisplay().getRealMetrics(metrics);
        } else {
            metrics = android.content.res.Resources.getSystem().getDisplayMetrics();
        }

        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int x1 = (int) (pX1 * width);
        int y1 = (int) (pY1 * height);
        int x2 = (int) (pX2 * width);
        int y2 = (int) (pY2 * height);

        android.util.Log.d(TAG, "=> NHẬN LỆNH SWIPE TỪ WEB: Vuốt từ tọa độ (" + x1 + "px, " + y1 + "px) ĐẾN (" + x2 + "px, " + y2 + "px)");

        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        android.accessibilityservice.GestureDescription.Builder builder = new android.accessibilityservice.GestureDescription.Builder();
        builder.addStroke(new android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 300));
        asbl.dispatchGesture(builder.build(), null, null);
    }
}
