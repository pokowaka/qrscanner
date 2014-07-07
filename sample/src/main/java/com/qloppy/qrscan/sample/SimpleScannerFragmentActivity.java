package com.qloppy.qrscan.sample;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.qloppy.qrscan.QRCodeScannerView;

public class SimpleScannerFragmentActivity extends ActionBarActivity implements QRCodeScannerView.ResultHandler {
    private QRCodeScannerView mScannerView;
    private TextView mTextScan;
    private TextView mTextScanSpeed;


    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mTextScanSpeed.setText("FPS: " + mScannerView.getFps());
            handler.postDelayed(this, 100);
        }
    };

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_simple_scanner_fragment);
        mScannerView = (QRCodeScannerView) findViewById(R.id.qrScan);
        mScannerView.setAutoFocus(true);
        mTextScan = (TextView) findViewById(R.id.textScan);
        mTextScanSpeed = (TextView) findViewById(R.id.textScanSpeed);
        handler.postDelayed(runnable, 100);
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void handleQrResult(String rawResult) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
        }

        // Vibrate for 500 milliseconds
        try {
            Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(500);
        } catch (Exception e) {
        }


        mTextScan.setText("Scanned: " + rawResult);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }
}