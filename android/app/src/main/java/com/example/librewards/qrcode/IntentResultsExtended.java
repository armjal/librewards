package com.example.librewards.qrcode;

import com.google.zxing.integration.android.IntentIntegrator;

public class IntentResultsExtended {

    /**
     * <p>Encapsulates the result of a barcode scan invoked through {@link IntentIntegrator}.</p>
     *
     * @author Sean Owen
     */
    private final String contents;
    private final String formatName;
    private final byte[] rawBytes;
    private final Integer orientation;
    private final String errorCorrectionLevel;
    private final String barcodeImagePath;
    IntentResultsExtended() {
        this(null, null, null, null, null, null);
    }
    IntentResultsExtended(String contents,
                 String formatName,
                 byte[] rawBytes,
                 Integer orientation,
                 String errorCorrectionLevel,
                 String barcodeImagePath) {
        this.contents = contents;
        this.formatName = formatName;
        this.rawBytes = rawBytes;
        this.orientation = orientation;
        this.errorCorrectionLevel = errorCorrectionLevel;
        this.barcodeImagePath = barcodeImagePath;
    }

    /**
     * @return raw content of barcode
     */
    public String getContents() {
        return contents;
    }

    /**
     * @return name of format, like "QR_CODE", "UPC_A". See {@code BarcodeFormat} for more format names.
     */
    public String getFormatName() {
        return formatName;
    }

    /**
     * @return raw bytes of the barcode content, if applicable, or null otherwise
     */
    public byte[] getRawBytes() {
        return rawBytes;
    }

    /**
     * @return rotation of the image, in degrees, which resulted in a successful scan. May be null.
     */
    public Integer getOrientation() {
        return orientation;
    }

    /**
     * @return name of the error correction level used in the barcode, if applicable
     */
    public String getErrorCorrectionLevel() {
        return errorCorrectionLevel;
    }

    /**
     * @return path to a temporary file containing the barcode image, if applicable, or null otherwise
     */
    public String getBarcodeImagePath() {
        return barcodeImagePath;
    }

    @Override
    public String toString() {
        int rawBytesLength = rawBytes == null ? 0 : rawBytes.length;
        return "Format: " + formatName + '\n' +
                "Contents: " + contents + '\n' +
                "Raw bytes: (" + rawBytesLength + " bytes)\n" +
                "Orientation: " + orientation + '\n' +
                "EC level: " + errorCorrectionLevel + '\n' +
                "Barcode image: " + barcodeImagePath + '\n';
    }
}


