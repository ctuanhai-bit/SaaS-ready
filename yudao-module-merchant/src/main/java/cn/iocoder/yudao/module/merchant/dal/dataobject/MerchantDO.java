package cn.iocoder.yudao.module.merchant.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@TableName("biz_merchant")
@KeySequence("biz_merchant_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MerchantDO extends TenantBaseDO {

    private Long id;
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
