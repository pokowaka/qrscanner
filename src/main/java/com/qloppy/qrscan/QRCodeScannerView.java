package com.qloppy.qrscan;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
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
    private Camera mCamera;
    private CameraPreview mPreview;
    private ViewFinderView mFinder;
    private ImageScanner mScanner;
    private ResultHandler mResultHandler;

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

    public void startCamera() {
        mCamera = CameraUtils.getCameraInstance();
        if (mCamera != null) {
            mPreview.setCamera(mCamera, this);
            mPreview.initCameraPreview();
            mPreview.showCameraPreview();
        }
    }

    public void stopCamera() {
        if (mCamera != null) {
            mPreview.stopCameraPreview();
            mPreview.setCamera(null, null);
            mCamera.release();
            mCamera = null;
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

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d(TAG, "Got a frame");
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();
        int width = size.width;
        int height = size.height;

        if (DisplayUtils.getScreenOrientation(getContext()) == Configuration.ORIENTATION_PORTRAIT) {
            byte[] rotatedData = new byte[data.length];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++)
                    rotatedData[x * height + height - y - 1] = data[x + y * width];
            }
            int tmp = width;
            width = height;
            height = tmp;
            data = rotatedData;
        }

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
                mResultHandler.handleQrResult(qrCode);
            }
        } else {
            camera.setOneShotPreviewCallback(this);
        }
    }

    public interface ResultHandler {
        public void handleQrResult(String qrCode);
    }
    static {
        System.loadLibrary("iconv");
    }
}
