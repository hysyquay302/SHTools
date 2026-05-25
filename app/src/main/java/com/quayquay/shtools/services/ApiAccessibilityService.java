package com.quayquay.shtools.services;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.quayquay.hsq.tools.HSQConfig;
import com.quayquay.shtools.extention.ASInterface;
import com.quayquay.shtools.extention.LOG;


public class ApiAccessibilityService extends AccessibilityService {
    private static final String TAG = "ACCSBLT";
    private static ApiAccessibilityService sInstance = null;
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.currentThread().setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LOG.saveUncaughtExceptionLog(TAG,t,e);
            }
        });
        sInstance = this;
        ASBLBridgeService.setASBLInstance(getInstance());
        LOG.D(TAG,"onCreate");
    }

    static public AccessibilityService getInstance() {
        return sInstance;
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        sInstance  = null;
        ASBLBridgeService.setASBLInstance(getInstance());
    }
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // --- ĐOẠN MỚI THÊM: AUTO CLICK SCREEN CAST ---
        try {
            // Chỉ bắt sự kiện nếu popup đó là của hệ thống (SystemUI) để đỡ nặng máy
            if (event.getPackageName() != null && event.getPackageName().toString().contains("com.android.systemui")) {
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                if (rootNode != null) {
                    // Mảng chứa các từ khóa nút bấm (Hỗ trợ cả Tiếng Anh, Tiếng Việt và các bản Android khác nhau)
                    String[] keywords = {"Start now", "Bắt đầu ngay", "Bắt đầu", "START NOW"};

                    for (String keyword : keywords) {
                        java.util.List<AccessibilityNodeInfo> listNodes = rootNode.findAccessibilityNodeInfosByText(keyword);
                        for (AccessibilityNodeInfo node : listNodes) {
                            // Đảm bảo nó đúng là cái Nút (Button) thì mới click
                            if (node.getClassName() != null && node.getClassName().toString().contains("Button")) {
                                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                return; // Bấm xong thì thoát luôn cho nhẹ
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("AutoClick", "Lỗi Auto Click: " + e.getMessage());
        }
        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            String sourcePackageName = (String) event.getPackageName();
            Parcelable parcelable = event.getParcelableData();
            if (parcelable instanceof Notification == false) {
                if(event.getText().size() > 0) {

                    ASBLBridgeService.setLastToastMsg(sourcePackageName, String.valueOf(event.getText().get(0)));
                }
            }
        }

    }


    @Override
    protected void onServiceConnected() {
        LOG.I(TAG, "onServiceConnected");
        ASInterface.instance().init(this);
        android.content.SharedPreferences prefs = HSQConfig.getContext().getSharedPreferences("QQ_PREFS", Context.MODE_PRIVATE);
        boolean isForceStopped = prefs.getBoolean("isForceStopped", false);

        // Nếu KHÔNG bị Force Stop thì mới cho phép bật màn hình App lên
        if (!isForceStopped) {
            Intent intent = this.getPackageManager().getLaunchIntentForPackage(this.getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_FROM_BACKGROUND);
            startActivity(intent);
        }
    }



    @Override
    public void onLowMemory() {
        super.onLowMemory();
        System.gc();
    }

//    /***************************** For debug *****************************/
//    static int mDebugDepth = 0;
//    Rect rect = new Rect();
//
//    public void printAllViews(AccessibilityNodeInfo mNodeInfo) {
//        LOG.D("printAllViews", " ");
//        if (mNodeInfo == null) return;
//        String log = "";
//        for (int i = 0; i < mDebugDepth; i++) {
//            log += "->";
//        }
//
//        mNodeInfo.getBoundsInScreen(rect);
//        log += "(" + mNodeInfo.getText() + ":" + mNodeInfo.getContentDescription() +
//                " isChecked:" + mNodeInfo.isChecked() +
//                " isCheckable:" + mNodeInfo.isCheckable() +
//                " isSelected:" + mNodeInfo.isSelected() +
//                " isVisibleToUser:" + mNodeInfo.isVisibleToUser() +
//                " isClickable:" + mNodeInfo.isClickable() +
//                " isEnabled:" + mNodeInfo.isEnabled() +
//                " isFocusable:" + mNodeInfo.isFocusable() +
//                " isFocusable:" + mNodeInfo.isFocused() +
//                " isEditable: " + mNodeInfo.isEditable() + " " +
//                " ResourceName: " + mNodeInfo.toString() + "<-- " +
//                rect.left + ":" + rect.top + ":" + rect.width() + ":" + rect.height() + ")";
//
//        LOG.D(TAG, log);
//
//        if (mNodeInfo.getChildCount() < 1) return;
//        mDebugDepth++;
//
//        for (int i = 0; i < mNodeInfo.getChildCount(); i++) {
//            printAllViews(mNodeInfo.getChild(i));
//        }
//        mDebugDepth--;
//    }
}
