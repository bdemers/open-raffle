package org.openraffle.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.openraffle.domain.Participant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

@Service
public class QrCodeService {

    private final String publicUrl;

    public QrCodeService(@Value("${raffle.public-url}") String publicUrl) {
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    /** The URL a participant lands on after scanning their QR code. */
    public String wishlistUrl(Participant participant) {
        return publicUrl + "/p/" + participant.getToken();
    }

    public byte[] pngFor(Participant participant, int sizePx) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    wishlistUrl(participant), BarcodeFormat.QR_CODE, sizePx, sizePx,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException e) {
            throw new IllegalStateException("Could not encode QR code", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
