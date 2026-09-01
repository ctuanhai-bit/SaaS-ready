package cn.iocoder.yudao.module.infra.framework.file.core.utils;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageThumbnailUtilsTest {

    @Test
    void shouldCreateSmallerThumbnailAndKeepAspectRatio() throws IOException {
        BufferedImage source = new BufferedImage(2400, 1600, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(7L);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                source.setRGB(x, y, random.nextInt(0x1000000));
            }
        }
        byte[] original = writeJpeg(source);

        byte[] thumbnail = ImageThumbnailUtils.createJpegThumbnail(original, 600);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(thumbnail));

        assertEquals(600, decoded.getWidth());
        assertEquals(400, decoded.getHeight());
        assertTrue(thumbnail.length < original.length / 4);
    }

    @Test
    void shouldReturnNullForUnsupportedContent() throws IOException {
        assertNull(ImageThumbnailUtils.createJpegThumbnail("not-an-image".getBytes(), 600));
    }

    private static byte[] writeJpeg(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", output);
            return output.toByteArray();
        }
    }

}
