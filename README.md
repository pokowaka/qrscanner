qrscanner
=========

QRCode scanner for android, based on ZBar

This thing is super simple to use, just add the following piece of code
to your activity:

```java
   @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mScannerView = new QRCodeScannerView(this);
        setContentView(mScannerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(new QRCodeScannerView.ResultHandler() {

            @Override
            public void handleQrResult(String qrCode) {
              // Do something sensible with the detected qrCode
                
            }
        }); 
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }
```


I've tested this on a HTC Incredible, so it should work from Android
2.3.3 onwards.


