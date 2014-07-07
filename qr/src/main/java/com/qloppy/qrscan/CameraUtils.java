package com.qloppy.qrscan;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.concurrent.Semaphore;

public class CameraUtils {

    private static CameraHandlerThread mThread = null;
    private static Camera c = null;
    private static final String TAG = CameraUtils.class.getName();

    private static Camera retrieveCamera() {
        try {
            c = Camera.open(); // attempt to get a Camera instance
            Log.d(TAG, "retrieveCamera: Obtained: " + c);
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Log.e(TAG, "retrieveCamera: Failed to obtain camera: " + e, e);
            c = null;
        }
        return c; // returns null if camera is unavailable
    }

    public static synchronized Camera getCameraInstance() {
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
                    retrieveCamera();
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
