package com.qloppy.qrscan;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.concurrent.Semaphore;

public class CameraUtils {

    private static CameraHandlerThread mThread = null;
    private static Camera c = null;

    private static Camera openCamera() {
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            c = null;
        }
        return c; // returns null if camera is unavailable
    }

    public static Camera getCameraInstance() {
        if (mThread == null) {
            mThread = new CameraHandlerThread();
        }

        mThread.openCamera();

        return c;
    }

    public static boolean isFlashSupported(Context context) {
        PackageManager packageManager = context.getPackageManager();
        // if device support camera flash?
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            return true;
        }
        return false;
    }

    private static class CameraHandlerThread extends HandlerThread {
        Handler mHandler = null;
        Semaphore semaphore = new Semaphore(0);

        CameraHandlerThread() {
            super("CameraHandlerThread");
            start();

            mHandler = new Handler(getLooper());
        }

        void openCamera() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    openCamera();
                    semaphore.release();
                }
            });
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
            }
        }
    }
}
