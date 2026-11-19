package com.shareshelf.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Map;

/**
 * Turns text into a QR code drawn as an inline SVG (no image files, no extra libraries beyond
 * ZXing's core). The SVG scales to any screen, which keeps it scannable on a phone.
 */
public final class QrUtil {

    private QrUtil() {
    }

    /** @return an &lt;svg&gt; string; safe to print unescaped because only digits and rectangles are in it. */
    public static String toSvg(String text) {
        try {
            BitMatrix m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.MARGIN, 2, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
            int n = m.getWidth();
            StringBuilder path = new StringBuilder();
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    if (m.get(x, y)) {
                        path.append('M').append(x).append(' ').append(y).append("h1v1h-1z");
                    }
                }
            }
            return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + n + ' ' + n
                    + "\" shape-rendering=\"crispEdges\" role=\"img\" aria-label=\"QR code\">"
                    + "<rect width=\"" + n + "\" height=\"" + n + "\" fill=\"#fff\"/>"
                    + "<path d=\"" + path + "\" fill=\"#000\"/></svg>";
        } catch (WriterException e) {
            throw new IllegalArgumentException("Could not draw a QR code", e);
        }
    }
}
