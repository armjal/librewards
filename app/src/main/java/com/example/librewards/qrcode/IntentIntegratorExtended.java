package com.example.librewards.qrcode;

import android.app.Activity;
import android.content.Intent;

import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;

public class IntentIntegratorExtended extends IntentIntegrator{

    public IntentIntegratorExtended(Activity activity) {
        super(activity);

    }
    public void extendedInitiateScan(int requestCode){
        startActivityForResult(createScanIntent(),requestCode);
    }
    public static IntentResultsExtended extendedParseActivityResult(int requestCode, int resultCode, Intent intent, int finalRequestCode) {
        if (requestCode == finalRequestCode) {
            if (resultCode == Activity.RESULT_OK) {
                String contents = intent.getStringExtra(Intents.Scan.RESULT);
                String formatName = intent.getStringExtra(Intents.Scan.RESULT_FORMAT);
                byte[] rawBytes = intent.getByteArrayExtra(Intents.Scan.RESULT_BYTES);
                int intentOrientation = intent.getIntExtra(Intents.Scan.RESULT_ORIENTATION, Integer.MIN_VALUE);
                Integer orientation = intentOrientation == Integer.MIN_VALUE ? null : intentOrientation;
                String errorCorrectionLevel = intent.getStringExtra(Intents.Scan.RESULT_ERROR_CORRECTION_LEVEL);
                String barcodeImagePath = intent.getStringExtra(Intents.Scan.RESULT_BARCODE_IMAGE_PATH);
                return new IntentResultsExtended(contents,
                        formatName,
                        rawBytes,
                        orientation,
                        errorCorrectionLevel,
                        barcodeImagePath);
            }
            return new IntentResultsExtended();
        }
        return null;
    }
}

