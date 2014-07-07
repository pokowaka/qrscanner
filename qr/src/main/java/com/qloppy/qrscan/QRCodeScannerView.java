package com.qloppy.qrscan;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;


/**
 * Created by erwinj on 7/3/14.
 */
public class QRCodeScannerView extends FrameLayout implements Camera.PreviewCallback {
    private static final String TAG = QRCodeScannerView.class.getName();
    private static final int MAXSAMPLES = 10;
    private long mTicklist[] = new long[MAXSAMPLES];
    private Camera mCamera;
    private CameraPreview mPreview;
    private ViewFinderView mFinder;
    private ImageScanner mScanner;
    private ResultHandler mResultHandler;
    private int mTickindex = 0;
    private long mTicksum = 0;
    private long mLastScan = 0;


    public QRCodeScannerView(Context context) {
        super(context);
        setupLayout();
        setupScanner();
    }

    public QRCodeScannerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setupLayout();
        setupScanner();
    }

    public void setResultHandler(ResultHandler resultHandler) {
        this.mResultHandler = resultHandler;
    }

    public void setupScanner() {
        mScanner = new ImageScanner();

        // See http://zbar.sourceforge.net/iphone/sdkdoc/optimizing.html section 2.4.5.2
        mScanner.setConfig(0, Config.X_DENSITY, 1);
        mScanner.setConfig(0, Config.Y_DENSITY, 1);


        mScanner.setConfig(Symbol.NONE, Config.ENABLE, 0);
        mScanner.setConfig(Symbol.QRCODE, Config.ENABLE, 1);
    }

    public void setupLayout() {
        mFinder = new ViewFinderView(getContext());
        mFinder.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        mPreview = new CameraPreview(getContext());
        mPreview.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        addView(mPreview);
        addView(mFinder);
    }

    public synchronized void startCamera() {
        Log.d(TAG, "startCamera: " + (mCamera != null));

        // Well, we must already have started the camera.
        if (mCamera != null)
            return;

        mCamera = CameraUtils.getCameraInstance();
        if (mCamera != null) {
            try {
                mPreview.setCamera(mCamera, this);
                mPreview.initCameraPreview();
                mPreview.showCameraPreview();
            } catch (Exception e) {
                Log.e(TAG, "startCamera: Failure: " + e, e);
            }
        }
    }

    public synchronized void stopCamera() {
        Log.d(TAG, "stopCamera: " + (mCamera != null));
        if (mCamera != null) {
            try {
                mPreview.stopCameraPreview();
                mPreview.setCamera(null, null);
                mCamera.release();
                mCamera = null;
            } catch (Exception e) {
                Log.e(TAG, "stopCamera: Failure: " + e, e);
            }
        }
    }

    public boolean getFlash() {
        if (CameraUtils.isFlashSupported(getContext()) && mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public void setFlash(boolean flag) {
        if (CameraUtils.isFlashSupported(getContext()) && mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (flag) {
                if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                    return;
                }
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                    return;
                }
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            mCamera.setParameters(parameters);
        }
    }

    public void toggleFlash() {
        if (CameraUtils.isFlashSupported(getContext()) && mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }
            mCamera.setParameters(parameters);
        }
    }

    public void setAutoFocus(boolean state) {
        if (mPreview != null) {
            mPreview.setAutoFocus(state);
        }
    }

    public double getFps() {
        return 1 / ((double) mTicksum / MAXSAMPLES / 1000);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mLastScan > 0) {
            calcAverageTick(System.currentTimeMillis() - mLastScan);
        }
        mLastScan = System.currentTimeMillis();
        Log.d(TAG, "Got a frame, fps: " + getFps());
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();
        int width = size.width;
        int height = size.height;

        Image barcode = new Image(width, height, "Y800");
        barcode.setData(data);

        int result = mScanner.scanImage(barcode);

        if (result != 0) {
            stopCamera();
            if (mResultHandler != null) {
                SymbolSet syms = mScanner.getResults();
                String qrCode = "";
                for (Symbol sym : syms) {
                    String symData = sym.getData();
                    if (!TextUtils.isEmpty(symData)) {
                        qrCode = symData;
                        break;
                    }
                }

                // Now post the result back on the UI thread.
                final String qr = qrCode;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mResultHandler.handleQrResult(qr);
                    }
                });
            }
        } else {
            camera.addCallbackBuffer(data);
        }
    }

    private double calcAverageTick(long newtick) {
        mTicksum -= mTicklist[mTickindex];  /* subtract value falling off */
        mTicksum += newtick;              /* add new value */
        mTicklist[mTickindex] = newtick;   /* save new value so it can be subtracted later */
        if (++mTickindex == MAXSAMPLES)    /* inc buffer index */
            mTickindex = 0;

        return ((double) mTicksum / MAXSAMPLES);
    }

    public interface ResultHandler {
        public void handleQrResult(String qrCode);
    }

    static {
        System.loadLibrary("iconv");
    }
}
