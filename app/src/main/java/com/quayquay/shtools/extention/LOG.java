package com.quayquay.shtools.extention;

import android.util.Log;

import com.quayquay.shtools.BuildConfig;

import java.util.Calendar;
import java.util.Date;

public class LOG {
    final static int PID = android.os.Process.myPid();
    private static boolean m_debug = BuildConfig.DEBUG;


    public static void  D(String TAG, String content) {
        if(m_debug) {
            Log.d(TAG,"[" + getTime() + "]" + "[" + PID + "] [" + content + "]");
        }
    }

    public static void I(String TAG, String content) {
        if(m_debug) {
            Log.i(TAG,"[" + getTime() + "]" + "[" + PID + "] [" + content + "]");
        }
    }

    public static void E(String TAG, String content) {
        if(m_debug) {
            Log.e(TAG,"[" + getTime() + "]" + "[" + PID + "] [" + content + "]");
        }
    }

    public static void W(String TAG, String content) {
        if(m_debug) {
            Log.w(TAG,"[" + getTime() + "]" + "[" + PID + "] [" + content + "]");
        }
    }

    public static void printStackTrace(String TAG, Exception e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        if(m_debug) {
            e.printStackTrace();
        }
    }

    public static String getTime() {
        Date currentDate = Calendar.getInstance().getTime();
        int date = currentDate.getDate();
        int month = currentDate.getMonth();
        int year = currentDate.getYear();
        int hours = currentDate.getHours();
        int min = currentDate.getMinutes();
        int sec = currentDate.getSeconds();
        return (1900 + year) + "-" + (month + 1) + "-" + date + " " + hours + ":" + min + ":" + sec;
    }

    static public void saveUncaughtExceptionLog(String TAG, Thread t, Throwable e) {
        e.printStackTrace();
    }
}