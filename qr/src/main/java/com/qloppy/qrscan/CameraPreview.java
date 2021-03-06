package com.qloppy.qrscan;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.util.List;

class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";

    private Camera mCamera;
    private Handler mAutoFocusHandler;
    private boolean mPreviewing = true;
    private boolean mAutoFocus = true;
    private boolean mSurfaceCreated = false;
    private Camera.PreviewCallback mPreviewCallback;
    private int mBufferSize;

    public CameraPreview(Context context) {
        super(context);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCamera(Camera camera, Camera.PreviewCallback previewCallback) {
        mCamera = camera;
        mBufferSize = 0;
        mPreviewCallback = previewCallback;
        mAutoFocusHandler = new Handler();
    }

    public void initCameraPreview() {
        if (mCamera != null) {
            getHolder().addCallback(this);
            getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            if (mPreviewing) {
                requestLayout();
            } else {
                showCameraPreview();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mSurfaceCreated = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged: surfaceHolder: " + surfaceHolder + ", format: " + format + ", width: " + width + ", height: " + height);
        if (surfaceHolder.getSurface() == null) {
            return;
        }

        Log.d(TAG, "surfaceChanged: " + (surfaceHolder == this.getHolder()));
        stopCameraPreview();
        showCameraPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mSurfaceCreated = false;
        stopCameraPreview();
    }

    public void showCameraPreview() {
        Log.d(TAG, "showCameraPreview: " + (mCamera != null) + ", holder:" + getHolder().getSurface().isValid());
        if (mCamera != null && mSurfaceCreated && !mPreviewing) {
            try {
                mPreviewing = true;
                setupCameraParameters();
                mCamera.setPreviewDisplay(getHolder());
                mCamera.setPreviewCallbackWithBuffer(mPreviewCallback);
                mCamera.setDisplayOrientation(getDisplayOrientation());
                mCamera.startPreview();
                if (mAutoFocus && mSurfaceCreated) {
                    mCamera.autoFocus(autoFocusCB);
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
        }
    }


    public void stopCameraPreview() {
        Log.d(TAG, "stopCameraPreview");
        if (mCamera != null) {
            try {
                mPreviewing = false;
                mCamera.cancelAutoFocus();
                mCamera.setPreviewCallbackWithBuffer(null);
                mCamera.stopPreview();
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            if (mPreviewing) {
                stopCameraPreview();
                showCameraPreview();
            }
        }
    }


    public void setupCameraParameters() {
        Camera.Size optimalSize = getOptimalPreviewSize();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);
        List<int[]> ranges = parameters.getSupportedPreviewFpsRange();

        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        mAutoFocus = supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO);

        // Well, turns out that not every android device is respecting this..
        if (ranges != null) {
            // Let's boost the camera to go as fast as we can.
            int[] range = ranges.get(ranges.size() - 1);
            parameters.setPreviewFpsRange(range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX], range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
            mCamera.setParameters(parameters);
        }

        // Now calculate the needed buffer size
        int bufferSize = parameters.getPreviewSize().height * parameters.getPreviewSize().width *
                ImageFormat.getBitsPerPixel(parameters.getPreviewFormat());

        // Add one buffer, on orientation change we can add this multiple times, so we only
        // Update the buffer if we need a bigger one, the smaller one will be discarded automatically.
        if (bufferSize > mBufferSize) {
            mCamera.addCallbackBuffer(new byte[bufferSize]);
            mBufferSize = bufferSize;
        }
        Log.d(TAG, "setupCameraParameters: complete");
    }


    public int getDisplayOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private Camera.Size getOptimalPreviewSize() {
        if (mCamera == null) {
            return null;
        }

        List<Camera.Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
        int w = this.getWidth();
        int h = this.getHeight();

        Log.d(TAG, "getOptimalPreviewSize: looking for: " + w + "x" + h);


        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        Log.d(TAG, "Using preview size: " + optimalSize.width + "x" + optimalSize.height);
        return optimalSize;
    }

    public synchronized void setAutoFocus(boolean state) {
        if (mCamera != null && mPreviewing) {
            if (state == mAutoFocus) {
                return;
            }
            mAutoFocus = state;
            if (mAutoFocus) {
                Log.v(TAG, "Starting autofocus");
                mCamera.autoFocus(autoFocusCB);
            } else {
                Log.v(TAG, "Cancelling autofocus");
                mCamera.cancelAutoFocus();
            }
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (mCamera != null && mPreviewing && mAutoFocus && mSurfaceCreated) {
                mCamera.autoFocus(autoFocusCB);
            }
        }
    };

    // Mimic continuous auto-focusing
    Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            mAutoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };
}
