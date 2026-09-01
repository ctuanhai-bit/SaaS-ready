package cn.iocoder.yudao.module.merchant.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.merchant.controller.admin.vo.MerchantRespVO;
import cn.iocoder.yudao.module.merchant.controller.admin.vo.MerchantUpdateReqVO;
import cn.iocoder.yudao.module.merchant.dal.dataobject.MerchantDO;
import cn.iocoder.yudao.module.merchant.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.merchant.enums.ErrorCodeConstants.MERCHANT_CONTEXT_NOT_EXISTS;

@Tag(name = "管理后台 - 酒店资料")
@RestController
@RequestMapping("/merchant/context")
@Validated
public class MerchantController {

    @Resource
    private MerchantService merchantService;

    @GetMapping
    @Operation(summary = "获得当前酒店资料")
    @PreAuthorize("@ss.hasAnyPermissions('merchant:profile:query', 'booking:room-type:query', 'booking:order:query')")
    public CommonResult<MerchantRespVO> getMerchantContext() {
        return success(toRespVO(getCurrentMerchant()));
    }

    @GetMapping("/detail")
    @Operation(summary = "获得当前酒店资料详情")
    @PreAuthorize("@ss.hasPermission('merchant:profile:query')")
    public CommonResult<MerchantRespVO> getMerchantContextDetail() {
        return success(toRespVO(getCurrentMerchant()));
    }

    @PostMapping("/update")
    @Operation(summary = "更新当前酒店资料")
    @PreAuthorize("@ss.hasPermission('merchant:profile:update')")
    public CommonResult<MerchantRespVO> updateMerchantContext(
            @Valid @RequestBody MerchantContextChangeReqVO changeReqVO) {
        MerchantDO current = getCurrentMerchant();
        MerchantUpdateReqVO updateReqVO = new MerchantUpdateReqVO()
                .setId(current.getId())
                .setName(firstNonBlank(changeReqVO.getName(), current.getName()))
                .setBusinessType("hotel")
                .setLogoUrl(firstNonBlank(changeReqVO.getLogoUrl(), current.getLogoUrl()))
                .setCoverUrl(firstNonBlank(changeReqVO.getCoverUrl(), current.getCoverUrl()))
                .setImageUrls(firstNonBlank(changeReqVO.getImageUrls(), current.getImageUrls()))
                .setVideoUrl(firstNonBlank(changeReqVO.getVideoUrl(), current.getVideoUrl()))
                .setVideoCoverUrl(firstNonBlank(changeReqVO.getVideoCoverUrl(), current.getVideoCoverUrl()))
                .setThemeColor(firstNonBlank(changeReqVO.getThemeColor(), current.getThemeColor()))
                .setLicenseImageUrl(firstNonBlank(changeReqVO.getLicenseImageUrl(), current.getLicenseImageUrl()))
                .setQualificationImageUrl(firstNonBlank(changeReqVO.getQualificationImageUrl(), current.getQualificationImageUrl()))
                .setContactName(firstNonBlank(changeReqVO.getContactName(), current.getContactName()))
                .setContactMobile(firstNonBlank(changeReqVO.getContactMobile(), current.getContactMobile()))
                .setAddress(firstNonBlank(changeReqVO.getAddress(), current.getAddress()))
                .setLongitude(changeReqVO.getLongitude() == null ? current.getLongitude() : changeReqVO.getLongitude())
                .setLatitude(changeReqVO.getLatitude() == null ? current.getLatitude() : changeReqVO.getLatitude())
                .setDescription(firstNonBlank(changeReqVO.getDescription(), current.getDescription()));
        merchantService.updateMerchant(updateReqVO);
        return success(toRespVO(merchantService.getMerchant(current.getId())));
    }

    private MerchantDO getCurrentMerchant() {
        Long merchantId = merchantService.getCurrentMerchantId();
        if (merchantId == null) {
            throw exception(MERCHANT_CONTEXT_NOT_EXISTS);
        }
        return merchantService.getMerchant(merchantId);
    }

    private MerchantRespVO toRespVO(MerchantDO merchant) {
        return BeanUtils.toBean(merchant, MerchantRespVO.class);
    }

    private String firstNonBlank(String candidate, String fallback) {
        return candidate == null || candidate.trim().isEmpty() ? fallback : candidate.trim();
    }

    @Schema(description = "管理后台 - 当前酒店资料更新 Request VO")
    @Data
    public static class MerchantContextChangeReqVO {
        @Size(max = 100, message = "酒店名称不能超过 100 个字符")
        private String name;
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
        @Size(max = 255, message = "酒店地址不能超过 255 个字符")
        private String address;
        @DecimalMin(value = "-180", message = "经度不能小于 -180")
        @DecimalMax(value = "180", message = "经度不能大于 180")
        private BigDecimal longitude;
        @DecimalMin(value = "-90", message = "纬度不能小于 -90")
        @DecimalMax(value = "90", message = "纬度不能大于 90")
        private BigDecimal latitude;
        @Size(max = 500, message = "酒店简介不能超过 500 个字符")
        private String description;
    }

}
