package com.qloppy.qrscan.sample;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import com.qloppy.qrscan.QRCodeScannerView;


public class SimpleScannerActivity extends ActionBarActivity implements QRCodeScannerView.ResultHandler {
    private QRCodeScannerView mScannerView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mScannerView = new QRCodeScannerView(this);
        setContentView(mScannerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
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

        Toast.makeText(this, "Contents = " + rawResult, Toast.LENGTH_SHORT).show();
        mScannerView.startCamera();
    }
}
