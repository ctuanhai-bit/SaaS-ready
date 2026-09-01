package cn.iocoder.yudao.module.merchant.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 商户更新 Request VO")
@Data
@Accessors(chain = true)
public class MerchantUpdateReqVO {

    @Schema(description = "商户编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商户编号不能为空")
    private Long id;

    @Schema(description = "商户名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "商户名称不能为空")
    private String name;

    @Schema(description = "业务类型，社区版固定为 hotel")
    @NotBlank(message = "商户类型不能为空")
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

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactMobile;

    @Schema(description = "商户地址")
    @Size(max = 255, message = "商户地址不能超过 255 个字符")
    private String address;

    @Schema(description = "经度，使用微信地图可识别的坐标")
    @DecimalMin(value = "-180", message = "经度不能小于 -180")
    @DecimalMax(value = "180", message = "经度不能大于 180")
    private BigDecimal longitude;

    @Schema(description = "纬度，使用微信地图可识别的坐标")
    @DecimalMin(value = "-90", message = "纬度不能小于 -90")
    @DecimalMax(value = "90", message = "纬度不能大于 90")
    private BigDecimal latitude;

    @Schema(description = "酒店简介")
    @Size(max = 500, message = "酒店简介不能超过 500 个字符")
    private String description;

}
