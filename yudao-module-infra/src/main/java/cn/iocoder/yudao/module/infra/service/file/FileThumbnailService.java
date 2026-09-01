package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.module.infra.framework.file.core.utils.ImageThumbnailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates and caches thumbnails for stored images.
 */
@Service
@Slf4j
public class FileThumbnailService {

    private static final int MAX_CACHE_ENTRIES = 128;
    private static final long MAX_CACHE_BYTES = 64L * 1024 * 1024;

    @Resource
    private FileService fileService;

    private final Map<String, byte[]> cache = new LinkedHashMap<>(16, 0.75F, true);
    private long cacheBytes;

    public byte[] getThumbnail(Long configId, String path, int width) throws Exception {
        String cacheKey = configId + ":" + width + ":" + path;
        synchronized (cache) {
            byte[] cached = cache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        byte[] content = fileService.getFileContent(configId, path);
        if (content == null) {
            return null;
        }
        byte[] thumbnail;
        try {
            thumbnail = ImageThumbnailUtils.createJpegThumbnail(content, width);
        } catch (IOException ex) {
            log.warn("[getThumbnail][configId({}) path({}) 图片解码失败，返回原文件]", configId, path, ex);
            return content;
        }
        if (thumbnail == null) {
            return content;
        }
        putCache(cacheKey, thumbnail);
        return thumbnail;
    }

    private void putCache(String key, byte[] content) {
        if (content.length > MAX_CACHE_BYTES) {
            return;
        }
        synchronized (cache) {
            byte[] previous = cache.put(key, content);
            if (previous != null) {
                cacheBytes -= previous.length;
            }
            cacheBytes += content.length;
            Iterator<Map.Entry<String, byte[]>> iterator = cache.entrySet().iterator();
            while ((cache.size() > MAX_CACHE_ENTRIES || cacheBytes > MAX_CACHE_BYTES) && iterator.hasNext()) {
                Map.Entry<String, byte[]> eldest = iterator.next();
                cacheBytes -= eldest.getValue().length;
                iterator.remove();
            }
        }
    }

}
