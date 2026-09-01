package cn.iocoder.yudao.module.merchant.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 酒店资料 Response VO")
@Data
public class MerchantRespVO {

    private Long id;
    private Long tenantId;
    private String name;
    private String businessType;
    private String logoUrl;
    private String coverUrl;
    private String imageUrls;
    private String videoUrl;
    private String videoCoverUrl;
    private String themeColor;
    private String licenseImageUrl;
    private String qualificationImageUrl;
    private String contactName;
    private String contactMobile;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String description;
    private Integer status;

}
