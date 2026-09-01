package cn.iocoder.yudao.module.infra.framework.file.core.client.s3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class S3FileClientCdnTest {

    @Test
    void shouldReturnCdnUrlForPrivateOrigin() {
        S3FileClientConfig config = createConfig();
        config.setEnablePublicAccess(false);
        config.setCdnDomain("https://cdn.example.com/");
        config.setCdnPathPrefix("/hotel-pms/public");
        S3FileClient client = new S3FileClient(1L, config);
        client.init();

        assertEquals("https://cdn.example.com/hotel-pms/public/hotel/cover.jpg",
                client.presignGetUrl("hotel-pms/public/hotel/cover.jpg", null));
        assertEquals("https://cdn.example.com/hotel-pms/public/hotel/cover.jpg",
                client.presignGetUrl("https://cdn.example.com/hotel-pms/public/hotel/cover.jpg?version=2", null));
    }

    @Test
    void shouldNotExposeFilesOutsideCdnPath() {
        S3FileClientConfig config = createConfig();
        config.setEnablePublicAccess(true);
        config.setCdnDomain("https://cdn.example.com");
        config.setCdnPathPrefix("hotel-pms/public");
        S3FileClient client = new S3FileClient(1L, config);
        client.init();

        assertEquals("https://bucket.cos.ap-shanghai.myqcloud.com/20260721/license.jpg",
                client.presignGetUrl("20260721/license.jpg", null));
    }

    @Test
    void shouldKeepStorageDomainWhenCdnIsNotConfigured() {
        S3FileClientConfig config = createConfig();
        config.setEnablePublicAccess(true);
        S3FileClient client = new S3FileClient(1L, config);
        client.init();

        assertEquals("https://bucket.cos.ap-shanghai.myqcloud.com/merchant/cover.jpg",
                client.presignGetUrl("merchant/cover.jpg", null));
    }

    private static S3FileClientConfig createConfig() {
        S3FileClientConfig config = new S3FileClientConfig();
        config.setEndpoint("cos.ap-shanghai.myqcloud.com");
        config.setDomain("https://bucket.cos.ap-shanghai.myqcloud.com/");
        config.setBucket("bucket");
        config.setAccessKey("access-key");
        config.setAccessSecret("access-secret");
        config.setEnablePathStyleAccess(false);
        return config;
    }

}
