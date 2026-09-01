package cn.iocoder.yudao.module.infra.framework.file.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Image thumbnail utilities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageThumbnailUtils {

    /**
     * Creates a JPEG thumbnail without enlarging the source image.
     *
     * @return thumbnail bytes, or {@code null} when the content is not a supported image
     */
    public static byte[] createJpegThumbnail(byte[] content, int targetWidth) throws IOException {
        if (targetWidth <= 0) {
            throw new IllegalArgumentException("targetWidth must be greater than 0");
        }
        BufferedImage source;
        try (ByteArrayInputStream input = new ByteArrayInputStream(content)) {
            source = ImageIO.read(input);
        }
        if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
            return null;
        }

        int width = Math.min(targetWidth, source.getWidth());
        int height = Math.max(1, (int) Math.round((double) source.getHeight() * width / source.getWidth()));
        BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = thumbnail.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(thumbnail, "jpg", output)) {
                return null;
            }
            return output.toByteArray();
        }
    }

}
