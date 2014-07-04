package com.qloppy.qrscan.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

public class MainActivity extends ActionBarActivity {
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
    }

    public void launchSimpleActivity(View v) {
        Intent intent = new Intent(this, SimpleScannerActivity.class);
        startActivity(intent);
    }

    public void launchSimpleFragmentActivity(View v) {
        Intent intent = new Intent(this, SimpleScannerFragmentActivity.class);
        startActivity(intent);
    }


}