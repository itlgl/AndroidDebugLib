package com.itlgl.android.debuglib.utils;

import android.util.Log;

public class LogUtils {
    private static final String TAG = "AndroidDebugLib";
    private static boolean LOG = true;

    private LogUtils() {

    }

    public static void printLog(boolean log) {
        LOG = log;
    }

    public static void d(String message, Object... args) {
        if(!LOG) {
            return;
        }
        if(args != null && args.length > 0) {
            String log = null;
            try {
                log = String.format(message, args);
            } catch (Exception e) {
                // ignore exception
                log = message;
            }
            Log.d(TAG, log);
        } else {
            Log.d(TAG, message);
        }
    }

    public static void i(String message, Object... args) {
        if(!LOG) {
            return;
        }
        if(args != null && args.length > 0) {
            String log = null;
            try {
                log = String.format(message, args);
            } catch (Exception e) {
                // ignore exception
                log = message;
            }
            Log.i(TAG, log);
        } else {
            Log.i(TAG, message);
        }
    }

    public static void e(String message, Object... args) {
        if(!LOG) {
            return;
        }
        if(args != null && args.length > 0) {
            String log = null;
            try {
                log = String.format(message, args);
            } catch (Exception e) {
                // ignore exception
                log = message;
            }
            Log.e(TAG, log);
        } else {
            Log.e(TAG, message);
        }
    }
}
