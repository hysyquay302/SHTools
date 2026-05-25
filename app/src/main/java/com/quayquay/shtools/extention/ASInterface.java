package com.quayquay.shtools.extention;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;

import com.quayquay.shtools.IASBLInterface;
import com.quayquay.shtools.screendefinitions.ScreenInfo;
import com.quayquay.shtools.screendefinitions.ScreenNode;
import com.quayquay.shtools.services.ASBLBridgeService;
import com.quayquay.shtools.services.ApiAccessibilityService;

public class ASInterface {
    public static ASInterface instance = null;
    public static String TAG = "CGBInterface";
    public Context context;
    public IASBLInterface mService;
    public boolean mIsServiceConnected;

    public ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = IASBLInterface.Stub.asInterface(iBinder);
            mIsServiceConnected = true;
            LOG.I(TAG, " -------------- ASBLBridgeService connected --------------" );
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mIsServiceConnected = false;
            LOG.I(TAG, " -------------- ASBLBridgeService disconnected --------------" );
        }
    };

    public ASInterface() {
        LOG.D(TAG,"Created CGBInterface");
    }

    public static ASInterface instance() {
        if(instance == null) {
            instance = new ASInterface();
        }
        return instance;
    }

    public boolean init(Context context){
        this.context = context;
        if(!mIsServiceConnected) {
            bindASBLBridgeService();
        }
        return true;
    }

    public void bindASBLBridgeService() {
        Intent intent = new Intent();
        intent.setClassName(context.getPackageName(), ASBLBridgeService.class.getName());
        context.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public boolean clickByPos(int x, int y, boolean longPress) throws RemoteException {
        return mService.clickByPos(x, y, longPress);
    }

    public boolean doubleClickByPos(int x, int y) throws RemoteException {
        return mService.doubleClickByPos(x, y);
    }

    public boolean clickByComp(String screenID, String compId) throws RemoteException {
        return mService.clickByComp(screenID, compId);
    }

    public boolean swipe(int x1, int y1, int x2, int y2, int duration) throws RemoteException {
        return mService.swipe(x1, y1, x2, y2, duration);
    }

    public boolean openPackage(String pckg) throws RemoteException {
        return mService.openPackage(pckg);
    }

    public boolean inputText(String txt, ScreenNode targetObj, boolean delay) throws RemoteException {
        return mService.inputText(txt, targetObj, delay);
    }


    public boolean scrollForward() throws RemoteException {
        return mService.scrollForward();
    }

    public boolean scrollBackward() throws RemoteException {
        return mService.scrollBackward();
    }

    public boolean globalBack() throws RemoteException {
        return mService.globalBack();
    }
    public ScreenInfo detectScreen(String appName) throws RemoteException {
        return this.detectScreen(appName, false);
    }

    public ScreenInfo detectScreen(String appName, boolean includeNullNode) throws RemoteException {
        return mService.detectScreen(appName, includeNullNode);
    }

    public void updateKeywordDefinitions(String defitions) throws RemoteException {
        mService.updateKeywordDefinitions(defitions);
    }

    public String getCurrentForgroundPkg() throws RemoteException {
        return mService.getCurrentForgroundPkg();
    }

    public String getLastToastMessage(String packageName) throws RemoteException {
        return mService.getLastToastMessage(packageName);
    }
    public void clearToastCache(String packageName) throws RemoteException {
        mService.clearToastCache(packageName);
    }

    public boolean isKeyboardShown() throws RemoteException {
        return mService.isKeyboardShown();
    }

    public boolean performGlobalAction(int action) throws RemoteException {
        return mService.performGlobalAction(action);
    }


    static public boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        final String service = mContext.getPackageName() + "/" + ApiAccessibilityService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
            LOG.D(TAG, "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            LOG.E(TAG, "Error finding setting, default accessibility to not found: "
                    + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            LOG.D(TAG, "***ACCESSIBILITY IS ENABLED*** -----------------");
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();

                    LOG.D(TAG, "-------------- > accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        LOG.D(TAG, "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            LOG.D(TAG, "***ACCESSIBILITY IS DISABLED***");
        }
        return false;
    }
}
