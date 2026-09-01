package cn.iocoder.yudao.module.infra.framework.file.core.client.s3;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClientConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

/**
 * S3 文件客户端的配置类
 *
 * @author 芋道源码
 */
@Data
public class S3FileClientConfig implements FileClientConfig {

    public static final String ENDPOINT_QINIU = "qiniucs.com";
    public static final String ENDPOINT_ALIYUN = "aliyuncs.com";
    public static final String ENDPOINT_TENCENT = "myqcloud.com";
    public static final String ENDPOINT_VOLCES = "volces.com"; // 火山云（字节）

    /**
     * 节点地址
     * 1. MinIO：https://www.iocoder.cn/Spring-Boot/MinIO 。例如说，http://127.0.0.1:9000
     * 2. 阿里云：https://help.aliyun.com/document_detail/31837.html
     * 3. 腾讯云：https://cloud.tencent.com/document/product/436/6224
     * 4. 七牛云：https://developer.qiniu.com/kodo/4088/s3-access-domainname
     * 5. 华为云：https://console.huaweicloud.com/apiexplorer/#/endpoint/OBS
     * 6. 火山云：https://www.volcengine.com/docs/6349/107356
     */
    @NotNull(message = "endpoint 不能为空")
    private String endpoint;
    /**
     * 自定义域名
     * 1. MinIO：通过 Nginx 配置
     * 2. 阿里云：https://help.aliyun.com/document_detail/31836.html
     * 3. 腾讯云：https://cloud.tencent.com/document/product/436/11142
     * 4. 七牛云：https://developer.qiniu.com/kodo/8556/set-the-custom-source-domain-name
     * 5. 华为云：https://support.huaweicloud.com/usermanual-obs/obs_03_0032.html
     * 6. 火山云：https://www.volcengine.com/docs/6349/128983
     */
    @URL(message = "domain 必须是 URL 格式")
    private String domain;
    /**
     * CDN 加速域名。
     *
     * 与 domain 分开配置，避免私有 Bucket 使用 CDN 回源时，预签名请求错误地发送到 CDN 域名。
     * 配置后，上传接口返回该域名下的公开访问地址；对象存储读写仍使用 endpoint/domain。
     */
    @URL(message = "cdnDomain 必须是 URL 格式")
    private String cdnDomain;
    /**
     * 允许通过 CDN 公开访问的对象路径前缀。
     *
     * 证照等非公开文件不应放入该目录。
     */
    private String cdnPathPrefix;
    /**
     * 存储 Bucket
     */
    @NotNull(message = "bucket 不能为空")
    private String bucket;

    /**
     * 访问 Key
     * 1. MinIO：https://www.iocoder.cn/Spring-Boot/MinIO
     * 2. 阿里云：https://ram.console.aliyun.com/manage/ak
     * 3. 腾讯云：https://console.cloud.tencent.com/cam/capi
     * 4. 七牛云：https://portal.qiniu.com/user/key
     * 5. 华为云：https://support.huaweicloud.com/qs-obs/obs_qs_0005.html
     * 6. 火山云：https://console.volcengine.com/iam/keymanage/
     */
    @NotNull(message = "accessKey 不能为空")
    private String accessKey;
    /**
     * 访问 Secret
     */
    @NotNull(message = "accessSecret 不能为空")
    private String accessSecret;

    /**
     * 是否启用 PathStyle 访问
     */
    @NotNull(message = "enablePathStyleAccess 不能为空")
    private Boolean enablePathStyleAccess;

    /**
     * 是否公开访问
     *
     * true：公开访问，所有人都可以访问
     * false：私有访问，只有配置的 accessKey 才可以访问
     */
    @NotNull(message = "是否公开访问不能为空")
    private Boolean enablePublicAccess;

    /**
     * 区域
     * 1. AWS S3：https://docs.aws.amazon.com/general/latest/gr/s3.html 例如说，us-east-1、us-west-2
     * 2. MinIO：可以填任意值，通常使用 us-east-1
     * 3. 阿里云：不需要填写，会自动识别
     * 4. 腾讯云：不需要填写，会自动识别
     * 5. 七牛云：不需要填写，会自动识别
     * 6. 华为云：不需要填写，会自动识别
     * 7. 火山云：不需要填写，会自动识别
     */
    private String region;

    public void setDomain(String domain) {
        this.domain = normalizeDomain(domain);
    }

    public void setCdnDomain(String cdnDomain) {
        this.cdnDomain = normalizeDomain(cdnDomain);
    }

    public void setCdnPathPrefix(String cdnPathPrefix) {
        if (StrUtil.isBlank(cdnPathPrefix)) {
            this.cdnPathPrefix = null;
            return;
        }
        String normalized = StrUtil.removePrefix(cdnPathPrefix.trim(), StrUtil.SLASH);
        this.cdnPathPrefix = StrUtil.addSuffixIfNot(normalized, StrUtil.SLASH);
    }

    private static String normalizeDomain(String domain) {
        return StrUtil.isBlank(domain) ? null : StrUtil.removeSuffix(domain.trim(), StrUtil.SLASH);
    }

    @SuppressWarnings("RedundantIfStatement")
    @AssertTrue(message = "domain 不能为空")
    @JsonIgnore
    public boolean isDomainValid() {
        // 如果是七牛，必须带有 domain
        if (StrUtil.contains(endpoint, ENDPOINT_QINIU) && StrUtil.isEmpty(domain)) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("RedundantIfStatement")
    @AssertTrue(message = "配置 cdnDomain 时，cdnPathPrefix 不能为空")
    @JsonIgnore
    public boolean isCdnPathPrefixValid() {
        if (StrUtil.isNotEmpty(cdnDomain) && StrUtil.isEmpty(cdnPathPrefix)) {
            return false;
        }
        return true;
    }

}
