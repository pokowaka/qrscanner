package com.qloppy.qrscan.sample;

import android.os.Bundle;
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
        Toast.makeText(this, "Contents = " + rawResult, Toast.LENGTH_SHORT).show();
        mScannerView.startCamera();
    }
}
